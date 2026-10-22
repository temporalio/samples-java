package io.temporal.samples.nexussecurecredentialpropagation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nexusrpc.handler.HandlerException;
import io.temporal.api.common.v1.Payload;
import io.temporal.api.common.v1.Payloads;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.common.v1.WorkflowType;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.api.history.v1.NexusOperationScheduledEventAttributes;
import io.temporal.api.history.v1.WorkflowExecutionStartedEventAttributes;
import io.temporal.api.taskqueue.v1.TaskQueue;
import io.temporal.api.workflowservice.v1.StartWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.StartWorkflowExecutionResponse;
import io.temporal.client.BatchRequest;
import io.temporal.client.UpdateOptions;
import io.temporal.client.WithStartWorkflowOperation;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowUpdateStage;
import io.temporal.common.RetryOptions;
import io.temporal.common.interceptors.Header;
import io.temporal.common.interceptors.WorkflowClientCallsInterceptor;
import io.temporal.common.interceptors.WorkflowClientCallsInterceptorBase;
import io.temporal.common.interceptors.WorkflowClientInterceptorBase;
import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.nexussecurecredentialpropagation.caller.CredentialCallerWorkerInterceptor;
import io.temporal.samples.nexussecurecredentialpropagation.caller.CredentialContext;
import io.temporal.samples.nexussecurecredentialpropagation.caller.CredentialWorkflowClientInterceptor;
import io.temporal.samples.nexussecurecredentialpropagation.caller.TransferWorkflow;
import io.temporal.samples.nexussecurecredentialpropagation.caller.TransferWorkflowImpl;
import io.temporal.samples.nexussecurecredentialpropagation.common.CredentialEnvelope;
import io.temporal.samples.nexussecurecredentialpropagation.common.DemoIdentityProvider;
import io.temporal.samples.nexussecurecredentialpropagation.common.SampleConfig;
import io.temporal.samples.nexussecurecredentialpropagation.common.SampleKeysets;
import io.temporal.samples.nexussecurecredentialpropagation.common.SecureCredentialHeaders;
import io.temporal.samples.nexussecurecredentialpropagation.handler.CredentialHandlerWorkerInterceptor;
import io.temporal.samples.nexussecurecredentialpropagation.handler.JwtVerifier;
import io.temporal.samples.nexussecurecredentialpropagation.handler.TransferAuthorizationPolicy;
import io.temporal.samples.nexussecurecredentialpropagation.handler.TransferNexusServiceImpl;
import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;
import io.temporal.samples.nexussecurecredentialpropagation.tools.VerifyCredentialBinding;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** End-to-end tests of the credential protocol against the in-memory test server. */
public class SecureCredentialPropagationTest {
  private static final String NAMESPACE = "default";
  private static final String OTHER_ENDPOINT = "other-endpoint";
  private static final String ATTACKER_TASK_QUEUE = "attacker";

  @TempDir Path keyDir;

  private SampleKeysets keys;
  private TestWorkflowEnvironment env;
  private WorkflowClient client;
  private WorkerFactory attackerFactory;
  private DemoIdentityProvider idp;

  @BeforeEach
  public void setUp() throws Exception {
    keys = SampleKeysets.fromDirectory(keyDir);
    keys.generate();
    idp = new DemoIdentityProvider(keys.demoIdentityProviderSigner());

    env =
        TestWorkflowEnvironment.newInstance(
            TestEnvironmentOptions.newBuilder()
                .setWorkflowClientOptions(
                    WorkflowClientOptions.newBuilder()
                        .setNamespace(NAMESPACE)
                        .setDataConverter(SampleConfig.dataConverter(keys.payloadAead()))
                        .setInterceptors(
                            new CredentialWorkflowClientInterceptor(
                                NAMESPACE, keys.workflowCredentialEncrypt()))
                        .build())
                .setWorkerFactoryOptions(
                    WorkerFactoryOptions.newBuilder()
                        .setWorkerInterceptors(
                            new CredentialHandlerWorkerInterceptor(
                                keys.nexusCredentialDecrypt(),
                                new JwtVerifier(
                                    keys.demoIdentityProviderVerifier(),
                                    DemoIdentityProvider.ISSUER,
                                    DemoIdentityProvider.HANDLER_AUDIENCE),
                                Collections.singletonMap(
                                    TransferService.NAME + "/" + TransferService.TRANSFER,
                                    new TransferAuthorizationPolicy())),
                            // Registered last: outermost inbound, innermost outbound.
                            new CredentialCallerWorkerInterceptor(
                                keys.workflowCredentialDecrypt(),
                                keys.workflowCredentialEncrypt(),
                                keys.nexusCredentialEncrypt(),
                                SampleConfig.PROTECTED_ROUTES))
                        .build())
                .build());
    client = env.getWorkflowClient();
    env.createNexusEndpoint(SampleConfig.ENDPOINT, SampleConfig.HANDLER_TASK_QUEUE);
    env.createNexusEndpoint(OTHER_ENDPOINT, SampleConfig.HANDLER_TASK_QUEUE);

    Worker caller = env.newWorker(SampleConfig.CALLER_TASK_QUEUE);
    caller.registerWorkflowImplementationTypes(
        WorkflowImplementationOptions.newBuilder()
            .setNexusServiceOptions(
                Collections.singletonMap(
                    TransferService.NAME,
                    NexusServiceOptions.newBuilder().setEndpoint(SampleConfig.ENDPOINT).build()))
            .build(),
        TransferWorkflowImpl.class,
        TestWorkflows.SteerableTransferWorkflowImpl.class,
        TestWorkflows.TwoArgWorkflowImpl.class,
        TestWorkflows.NoArgWorkflowImpl.class,
        TestWorkflows.ContinuingWorkflowImpl.class,
        TestWorkflows.FlakyTransferWorkflowImpl.class,
        TestWorkflows.ParentWorkflowImpl.class,
        TestWorkflows.CancellingTransferWorkflowImpl.class);

    Worker handler = env.newWorker(SampleConfig.HANDLER_TASK_QUEUE);
    handler.registerWorkflowImplementationTypes(TestWorkflows.QuickSettleWorkflowImpl.class);
    handler.registerNexusServiceImplementation(
        new TransferNexusServiceImpl(keys.operationTokenMac()));

    // An attacker holding no keys, with a converter that sends captured bytes unchanged.
    WorkflowClient attackerClient =
        WorkflowClient.newInstance(
            env.getWorkflowServiceStubs(),
            WorkflowClientOptions.newBuilder().setNamespace(NAMESPACE).build());
    attackerFactory =
        WorkerFactory.newInstance(
            attackerClient,
            WorkerFactoryOptions.newBuilder()
                .setWorkerInterceptors(new TestWorkflows.AttackerHeaderInterceptor())
                .build());
    attackerFactory
        .newWorker(ATTACKER_TASK_QUEUE)
        .registerWorkflowImplementationTypes(TestWorkflows.AttackerWorkflowImpl.class);

    env.start();
    attackerFactory.start();
  }

  @AfterEach
  public void tearDown() {
    TestWorkflows.AttackerHeaderInterceptor.HEADERS.clear();
    attackerFactory.shutdownNow();
    env.close();
  }

  // ---------------------------------------------------------------------------------------------
  // Happy paths.

  @Test
  public void transfersOnBehalfOfVerifiedPrincipal() {
    TransferService.TransferInput input = transferInput("p-1");
    TransferService.TransferOutput output = runTransfer("caller-1", input, aliceCredential());
    assertEquals("SETTLED", output.getStatus());
    assertEquals("alice", output.getApprovedFor());
  }

  @Test
  public void historiesContainNoPlaintextCredential() {
    TransferService.TransferInput input = transferInput("p-plain");
    CredentialEnvelope credential = aliceCredential();
    runTransfer("caller-plain", input, credential);

    String jwt = credential.getCompactJwt();
    String jwtPayloadSegment = jwt.substring(jwt.indexOf('.') + 1, jwt.lastIndexOf('.'));
    List<String> histories = new ArrayList<>();
    histories.add(historyBytes("caller-plain"));
    histories.add(historyBytes(TransferNexusServiceImpl.resourceId(principal("alice"), input)));
    for (String history : histories) {
      assertFalse(history.contains(credential.getCompactJwt()));
      assertFalse(history.contains(jwtPayloadSegment));
      assertFalse(history.contains("alice"));
      assertFalse(history.contains("acme"));
    }
  }

  @Test
  public void signalWithStartCarriesCredential() {
    TransferService.TransferInput input = transferInput("p-sws");
    TestWorkflows.SteerableTransferWorkflow workflow =
        client.newWorkflowStub(
            TestWorkflows.SteerableTransferWorkflow.class, callerOptions("caller-sws"));
    CredentialContext.callWith(
        aliceCredential(),
        () -> {
          BatchRequest request = client.newSignalWithStartRequest();
          request.add(workflow::transfer, input);
          request.add(workflow::ping, "hello");
          return client.signalWithStart(request);
        });
    assertEquals(
        "SETTLED",
        WorkflowStub.fromTyped(workflow)
            .getResult(TransferService.TransferOutput.class)
            .getStatus());
  }

  @Test
  public void updateWithStartCarriesCredential() {
    TransferService.TransferInput input = transferInput("p-uws");
    TestWorkflows.SteerableTransferWorkflow workflow =
        client.newWorkflowStub(
            TestWorkflows.SteerableTransferWorkflow.class,
            WorkflowOptions.newBuilder(callerOptions("caller-uws"))
                .setWorkflowIdConflictPolicy(
                    io.temporal.api.enums.v1.WorkflowIdConflictPolicy
                        .WORKFLOW_ID_CONFLICT_POLICY_FAIL)
                .build());
    CredentialContext.callWith(
        aliceCredential(),
        () ->
            WorkflowClient.startUpdateWithStart(
                workflow::ack,
                "hello",
                UpdateOptions.<String>newBuilder()
                    .setWaitForStage(WorkflowUpdateStage.COMPLETED)
                    .build(),
                new WithStartWorkflowOperation<>(workflow::transfer, input)));
    assertEquals(
        "SETTLED",
        WorkflowStub.fromTyped(workflow)
            .getResult(TransferService.TransferOutput.class)
            .getStatus());
  }

  @Test
  public void credentialSurvivesContinueAsNewToAnotherTypeAndRetry() {
    TransferService.TransferInput input = transferInput("p-can");
    TestWorkflows.ContinuingWorkflow workflow =
        client.newWorkflowStub(
            TestWorkflows.ContinuingWorkflow.class,
            WorkflowOptions.newBuilder(callerOptions("caller-can"))
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumAttempts(3)
                        .build())
                .build());
    CredentialContext.callWith(
        aliceCredential(), () -> WorkflowClient.start(workflow::transfer, input));
    TransferService.TransferOutput output =
        client.newUntypedWorkflowStub("caller-can").getResult(TransferService.TransferOutput.class);
    assertEquals("SETTLED", output.getStatus());
    // The final run is the retry of the continued-as-new run, carrying the continue-as-new header.
    WorkflowExecutionStartedEventAttributes last = startedEvent("caller-can");
    assertEquals("FlakyTransferWorkflow", last.getWorkflowType().getName());
    assertEquals(2, last.getAttempt());
  }

  @Test
  public void childWorkflowsDoNotReceiveTheCredential() {
    TestWorkflows.ParentWorkflow parent =
        client.newWorkflowStub(TestWorkflows.ParentWorkflow.class, callerOptions("parent"));
    CredentialContext.callWith(aliceCredential(), () -> parent.run("child"));
    assertTrue(
        startedEvent("parent").getHeader().containsFields(SecureCredentialHeaders.WORKFLOW_HEADER));
    assertFalse(
        startedEvent("child").getHeader().containsFields(SecureCredentialHeaders.WORKFLOW_HEADER));
  }

  @Test
  public void cancellationUsesTheSignedToken() {
    TransferService.TransferInput input = transferInput("hold-1");
    TestWorkflows.CancellingTransferWorkflow workflow =
        client.newWorkflowStub(
            TestWorkflows.CancellingTransferWorkflow.class, callerOptions("caller-cancel"));
    String result =
        CredentialContext.callWith(aliceCredential(), () -> workflow.transferThenCancel(input));
    assertTrue(result.startsWith("cancelled:"), result);

    String backingId = TransferNexusServiceImpl.resourceId(principal("alice"), input);
    WorkflowFailedException failure =
        assertThrows(
            WorkflowFailedException.class,
            () -> client.newUntypedWorkflowStub(backingId).getResult(Object.class));
    assertTrue(failure.getCause() instanceof io.temporal.failure.CanceledFailure);
  }

  // ---------------------------------------------------------------------------------------------
  // Verifier tool.

  @Test
  public void verifierConfirmsBindingInRealHistory() {
    TransferService.TransferInput input = transferInput("p-verify");
    runTransfer("caller-verify", input, aliceCredential());

    List<VerifyCredentialBinding.Result> results =
        verifier().verify(NAMESPACE, "caller-verify", events("caller-verify"));
    assertEquals(2, results.size());
    assertEquals("WorkflowExecutionStarted", results.get(0).getEventType());
    assertEquals("NexusOperationScheduled", results.get(1).getEventType());
    assertTrue(results.stream().allMatch(VerifyCredentialBinding.Result::passed));
  }

  @Test
  public void verifierConfirmsContinueAsNewBinding() {
    credentialSurvivesContinueAsNewToAnotherTypeAndRetry();
    List<VerifyCredentialBinding.Result> results =
        verifier().verify(NAMESPACE, "caller-can", events("caller-can"));
    assertEquals(2, results.size());
    assertTrue(results.stream().allMatch(VerifyCredentialBinding.Result::passed));
  }

  @Test
  public void verifierDetectsSubstitutedArguments() {
    TransferService.TransferInput input = transferInput("p-verify-tamper");
    runTransfer("caller-verify-tamper", input, aliceCredential());

    List<HistoryEvent> history = new ArrayList<>(events("caller-verify-tamper"));
    HistoryEvent started = history.get(0);
    WorkflowExecutionStartedEventAttributes attributes =
        started.getWorkflowExecutionStartedEventAttributes().toBuilder()
            .setInput(Payloads.newBuilder().addPayloads(reencrypt("caller-verify-tamper", "x")))
            .build();
    history.set(
        0, started.toBuilder().setWorkflowExecutionStartedEventAttributes(attributes).build());

    List<VerifyCredentialBinding.Result> results =
        verifier().verify(NAMESPACE, "caller-verify-tamper", history);
    assertFalse(results.get(0).passed());
    assertTrue(results.get(1).passed());
  }

  private VerifyCredentialBinding verifier() {
    return new VerifyCredentialBinding(
        keys.workflowCredentialDecrypt(), keys.nexusCredentialDecrypt());
  }

  // ---------------------------------------------------------------------------------------------
  // Start hop attacks.

  @Test
  public void startHeaderIsBoundToTheExactArgumentList() {
    TestWorkflows.TwoArgWorkflow workflow =
        client.newWorkflowStub(TestWorkflows.TwoArgWorkflow.class, callerOptions("two-args"));
    assertEquals(
        "a,b", CredentialContext.callWith(aliceCredential(), () -> workflow.join("a", "b")));

    WorkflowExecutionStartedEventAttributes victim = startedEvent("two-args");
    List<Payload> args = victim.getInput().getPayloadsList();

    // An exact copy into a new run of the same ID is accepted (replay case 4).
    assertEquals(
        "a,b", rawStart("two-args", "TwoArgWorkflow", victim, args).getResult(String.class));

    List<List<Payload>> tampered = new ArrayList<>();
    tampered.add(listOf(args.get(1), args.get(0))); // Reordered.
    tampered.add(listOf(args.get(0), args.get(0))); // Duplicated.
    tampered.add(listOf(args.get(0))); // Truncated.
    tampered.add(listOf(args.get(0), reencrypt("two-args", "b"))); // Substituted.
    for (List<Payload> payloads : tampered) {
      WorkflowFailedException failure =
          assertThrows(
              WorkflowFailedException.class,
              () ->
                  rawStart("two-args", "TwoArgWorkflow", victim, payloads).getResult(String.class));
      assertEquals(
          CredentialCallerWorkerInterceptor.CREDENTIAL_REJECTED,
          ((ApplicationFailure) failure.getCause()).getType());
    }
  }

  @Test
  public void copiedContinueAsNewHeaderIsRejectedInAFreshStart() {
    credentialSurvivesContinueAsNewToAnotherTypeAndRetry();
    // The last run in the chain was started by continue-as-new or retry and carries a CAN header.
    WorkflowExecutionStartedEventAttributes continued = startedEvent("caller-can");
    assertTrue(
        continued
            .getHeader()
            .getFieldsOrThrow(SecureCredentialHeaders.WORKFLOW_HEADER)
            .getData()
            .toStringUtf8()
            .startsWith("v1.can."));
    WorkflowFailedException failure =
        assertThrows(
            WorkflowFailedException.class,
            () ->
                rawStart(
                        "caller-can",
                        "FlakyTransferWorkflow",
                        continued,
                        continued.getInput().getPayloadsList())
                    .getResult(Object.class));
    assertEquals(
        CredentialCallerWorkerInterceptor.CREDENTIAL_REJECTED,
        ((ApplicationFailure) failure.getCause()).getType());
  }

  @Test
  public void startsWithoutArgumentsCannotCarryACredential() {
    TestWorkflows.NoArgWorkflow workflow =
        client.newWorkflowStub(TestWorkflows.NoArgWorkflow.class, callerOptions("no-args"));
    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () ->
                CredentialContext.callWith(
                    aliceCredential(), () -> WorkflowClient.start(workflow::run)));
    assertTrue(hasCause(e, IllegalArgumentException.class), () -> chain(e));
  }

  @Test
  public void innerInterceptorCopyingHeadersFailsClosed() {
    // An interceptor inside the credential interceptor that replaces the header map means the SDK
    // never sees the sealed header.
    WorkflowClient misconfigured =
        WorkflowClient.newInstance(
            env.getWorkflowServiceStubs(),
            WorkflowClientOptions.newBuilder(client.getOptions())
                .setInterceptors(
                    new HeaderCopyingClientInterceptor(),
                    new CredentialWorkflowClientInterceptor(
                        NAMESPACE, keys.workflowCredentialEncrypt()))
                .build());
    TransferService.TransferInput input = transferInput("p-copy");
    TransferWorkflow workflow =
        misconfigured.newWorkflowStub(TransferWorkflow.class, callerOptions("caller-copy"));
    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () ->
                CredentialContext.callWith(
                    aliceCredential(), () -> WorkflowClient.start(workflow::transfer, input)));
    assertTrue(hasCause(e, IllegalStateException.class));
  }

  // ---------------------------------------------------------------------------------------------
  // Nexus hop attacks.

  @Test
  public void exactNexusReplayWhileRunningAttachesToTheSameResource() throws InterruptedException {
    TransferService.TransferInput input = transferInput("hold-replay");
    TransferWorkflow victim =
        client.newWorkflowStub(TransferWorkflow.class, callerOptions("victim-running"));
    CredentialContext.callWith(
        aliceCredential(), () -> WorkflowClient.start(victim::transfer, input));
    String victimToken = awaitStartedToken("victim-running");
    NexusOperationScheduledEventAttributes scheduled = scheduledEvent("victim-running");

    TestWorkflows.AttackerHeaderInterceptor.HEADERS.putAll(scheduled.getNexusHeaderMap());
    assertEquals("started:" + victimToken, attack(SampleConfig.ENDPOINT, scheduled.getInput()));
    assertEquals(1, backingStarts(input));
  }

  @Test
  public void exactNexusReplayAfterCompletionCausesNoSecondEffect() {
    TransferService.TransferInput input = transferInput("p-replay");
    runTransfer("victim-replay", input, aliceCredential());
    NexusOperationScheduledEventAttributes victim = scheduledEvent("victim-replay");

    TestWorkflows.AttackerHeaderInterceptor.HEADERS.putAll(victim.getNexusHeaderMap());
    // The backing Workflow ID cannot be reused, so the replay is refused instead of re-running.
    assertEquals("rejected:BAD_REQUEST", attack(SampleConfig.ENDPOINT, victim.getInput()));
    assertEquals(1, backingStarts(input));
  }

  @Test
  public void nexusHeaderCannotBePairedWithAnotherInput() {
    TransferService.TransferInput inputA = transferInput("p-a");
    TransferService.TransferInput inputB = transferInput("p-b");
    runTransfer("victim-a", inputA, aliceCredential());
    runTransfer("victim-b", inputB, aliceCredential());

    TestWorkflows.AttackerHeaderInterceptor.HEADERS.putAll(
        scheduledEvent("victim-a").getNexusHeaderMap());
    assertEquals(
        "rejected:UNAUTHENTICATED",
        attack(SampleConfig.ENDPOINT, scheduledEvent("victim-b").getInput()));
  }

  @Test
  public void exactNexusPairCannotMoveToAnotherEndpoint() {
    TransferService.TransferInput input = transferInput("p-route");
    runTransfer("victim-route", input, aliceCredential());
    NexusOperationScheduledEventAttributes victim = scheduledEvent("victim-route");

    TestWorkflows.AttackerHeaderInterceptor.HEADERS.putAll(victim.getNexusHeaderMap());
    // The input ciphertext is bound to its endpoint, so the codec refuses it.
    assertEquals("rejected:BAD_REQUEST", attack(OTHER_ENDPOINT, victim.getInput()));
  }

  @Test
  public void nexusInputWithoutCredentialIsRejected() {
    TransferService.TransferInput input = transferInput("p-nocred");
    runTransfer("victim-nocred", input, aliceCredential());
    assertEquals(
        "rejected:UNAUTHENTICATED",
        attack(SampleConfig.ENDPOINT, scheduledEvent("victim-nocred").getInput()));
  }

  // ---------------------------------------------------------------------------------------------
  // Authorization.

  @Test
  public void subjectMismatchIsUnauthorized() {
    TransferService.TransferInput input =
        new TransferService.TransferInput("acme", "bob", "p-subject", "bob", 25_00);
    assertEquals(
        HandlerException.ErrorType.UNAUTHORIZED,
        transferFailure("caller-subject", input, aliceCredential()));
  }

  @Test
  public void tokenForAnotherAudienceIsUnauthenticated() {
    TransferService.TransferInput input = transferInput("p-aud");
    String jwt =
        idp.issue(
            "alice",
            "acme",
            TransferService.REQUIRED_SCOPE,
            "some-other-service",
            java.time.Instant.now(),
            Duration.ofMinutes(5));
    assertEquals(
        HandlerException.ErrorType.UNAUTHENTICATED,
        transferFailure("caller-aud", input, new CredentialEnvelope(jwt)));
  }

  // ---------------------------------------------------------------------------------------------
  // Helpers.

  private static TransferService.TransferInput transferInput(String paymentId) {
    return new TransferService.TransferInput("acme", "alice", paymentId, "bob", 25_00);
  }

  /** A credential for alice with the scope the handler requires. */
  private CredentialEnvelope aliceCredential() {
    String jwt = idp.issue("alice", "acme", TransferService.REQUIRED_SCOPE, Duration.ofMinutes(5));
    return new CredentialEnvelope(jwt);
  }

  private static io.temporal.samples.nexussecurecredentialpropagation.common.EndUserPrincipal
      principal(String subject) {
    return new io.temporal.samples.nexussecurecredentialpropagation.common.EndUserPrincipal(
        DemoIdentityProvider.ISSUER,
        subject,
        "acme",
        Collections.singleton(TransferService.REQUIRED_SCOPE));
  }

  private static WorkflowOptions callerOptions(String workflowId) {
    return WorkflowOptions.newBuilder()
        .setWorkflowId(workflowId)
        .setTaskQueue(SampleConfig.CALLER_TASK_QUEUE)
        .build();
  }

  private TransferService.TransferOutput runTransfer(
      String workflowId, TransferService.TransferInput input, CredentialEnvelope credential) {
    TransferWorkflow workflow =
        client.newWorkflowStub(TransferWorkflow.class, callerOptions(workflowId));
    return CredentialContext.callWith(credential, () -> workflow.transfer(input));
  }

  private HandlerException.ErrorType transferFailure(
      String workflowId, TransferService.TransferInput input, CredentialEnvelope credential) {
    WorkflowFailedException failure =
        assertThrows(
            WorkflowFailedException.class, () -> runTransfer(workflowId, input, credential));
    for (Throwable t = failure; t != null; t = t.getCause()) {
      if (t instanceof HandlerException) {
        return ((HandlerException) t).getErrorType();
      }
    }
    throw new AssertionError("No handler failure in " + failure);
  }

  private String attack(String endpoint, Payload capturedInput) {
    WorkflowClient attackerClient =
        WorkflowClient.newInstance(
            env.getWorkflowServiceStubs(),
            WorkflowClientOptions.newBuilder().setNamespace(NAMESPACE).build());
    TestWorkflows.AttackerWorkflow attacker =
        attackerClient.newWorkflowStub(
            TestWorkflows.AttackerWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("attacker-" + UUID.randomUUID())
                .setTaskQueue(ATTACKER_TASK_QUEUE)
                .build());
    return attacker.replay(endpoint, capturedInput.toByteArray());
  }

  private String historyBytes(String workflowId) {
    return new String(
        client.fetchHistory(workflowId).getHistory().toByteArray(), StandardCharsets.ISO_8859_1);
  }

  private List<HistoryEvent> events(String workflowId) {
    return client.fetchHistory(workflowId).getEvents();
  }

  private WorkflowExecutionStartedEventAttributes startedEvent(String workflowId) {
    return events(workflowId).get(0).getWorkflowExecutionStartedEventAttributes();
  }

  private NexusOperationScheduledEventAttributes scheduledEvent(String workflowId) {
    for (HistoryEvent event : events(workflowId)) {
      if (event.hasNexusOperationScheduledEventAttributes()) {
        return event.getNexusOperationScheduledEventAttributes();
      }
    }
    throw new AssertionError("No Nexus operation scheduled in " + workflowId);
  }

  private String awaitStartedToken(String workflowId) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 10_000;
    while (System.currentTimeMillis() < deadline) {
      for (HistoryEvent event : events(workflowId)) {
        if (event.hasNexusOperationStartedEventAttributes()) {
          return event.getNexusOperationStartedEventAttributes().getOperationToken();
        }
      }
      Thread.sleep(100);
    }
    throw new AssertionError("No Nexus operation started in " + workflowId);
  }

  private long backingStarts(TransferService.TransferInput input) {
    String backingId = TransferNexusServiceImpl.resourceId(principal("alice"), input);
    return events(backingId).stream()
        .filter(e -> e.getEventType() == EventType.EVENT_TYPE_WORKFLOW_EXECUTION_STARTED)
        .count();
  }

  /** Starts a run directly through the service, as an attacker copying history would. */
  private WorkflowStub rawStart(
      String workflowId,
      String workflowType,
      WorkflowExecutionStartedEventAttributes copiedFrom,
      List<Payload> args) {
    StartWorkflowExecutionResponse response =
        env.getWorkflowServiceStubs()
            .blockingStub()
            .startWorkflowExecution(
                StartWorkflowExecutionRequest.newBuilder()
                    .setNamespace(NAMESPACE)
                    .setWorkflowId(workflowId)
                    .setWorkflowType(WorkflowType.newBuilder().setName(workflowType))
                    .setTaskQueue(TaskQueue.newBuilder().setName(SampleConfig.CALLER_TASK_QUEUE))
                    .setInput(Payloads.newBuilder().addAllPayloads(args))
                    .setHeader(copiedFrom.getHeader())
                    .setRequestId(UUID.randomUUID().toString())
                    .setIdentity("attacker")
                    .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
                    .build());
    return client.newUntypedWorkflowStub(
        WorkflowExecution.newBuilder()
            .setWorkflowId(workflowId)
            .setRunId(response.getRunId())
            .build(),
        java.util.Optional.of(workflowType));
  }

  private Payload reencrypt(String workflowId, String value) {
    return client
        .getOptions()
        .getDataConverter()
        .withContext(
            new io.temporal.payload.context.WorkflowSerializationContext(NAMESPACE, workflowId))
        .toPayload(value)
        .get();
  }

  private static List<Payload> listOf(Payload... payloads) {
    List<Payload> list = new ArrayList<>();
    Collections.addAll(list, payloads);
    return list;
  }

  private static String chain(Throwable e) {
    StringBuilder b = new StringBuilder();
    for (Throwable t = e; t != null; t = t.getCause()) {
      b.append(t.getClass().getName()).append(": ").append(t.getMessage()).append(" <- ");
    }
    return b.toString();
  }

  private static boolean hasCause(Throwable e, Class<? extends Throwable> type) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (type.isInstance(t)) {
        return true;
      }
    }
    return false;
  }

  /** Replaces the header with a copy, as a misordered propagation interceptor might. */
  private static final class HeaderCopyingClientInterceptor extends WorkflowClientInterceptorBase {
    @Override
    public WorkflowClientCallsInterceptor workflowClientCallsInterceptor(
        WorkflowClientCallsInterceptor next) {
      return new WorkflowClientCallsInterceptorBase(next) {
        @Override
        public WorkflowStartOutput start(WorkflowStartInput input) {
          return super.start(
              new WorkflowStartInput(
                  input.getWorkflowId(),
                  input.getWorkflowType(),
                  new Header(new HashMap<>(input.getHeader().getValues())),
                  input.getArguments(),
                  input.getOptions()));
        }
      };
    }
  }
}
