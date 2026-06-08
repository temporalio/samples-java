package io.temporal.samples.nexusstandalone;

import io.temporal.api.enums.v1.NexusOperationExecutionStatus;
import io.temporal.client.*;
import io.temporal.common.converter.GlobalDataConverter;
import io.temporal.common.interceptors.NexusClientCallsInterceptor;
import io.temporal.common.interceptors.NexusClientCallsInterceptorBase;
import io.temporal.common.interceptors.NexusClientInterceptor;
import io.temporal.samples.nexusstandalone.service.ClientOptions;
import io.temporal.samples.nexusstandalone.service.GreetingIds;
import io.temporal.samples.nexusstandalone.service.GreetingNexusService;
import io.temporal.samples.nexusstandalone.service.GreetingNexusService.GreetingInput;
import io.temporal.samples.nexusstandalone.service.GreetingNexusService.GreetingOutput;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Sample client for standalone Nexus operations — operations started and managed directly by a
// client rather than from within a workflow. Each capability is shown in its own method, called in
// turn from main(): executing an operation and reading its result, cancelling and terminating an
// operation, querying operations via Visibility, and configuring client options and interceptors.
public class StandaloneClientStarter {
  private static final Logger logger = LoggerFactory.getLogger(StandaloneClientStarter.class);

  // Must match the Nexus endpoint configured on the server (see README).
  public static final String ENDPOINT_NAME = "nexusstandalone-endpoint";

  // A per-run suffix appended to workflow-backed operation names so their backing workflow IDs are
  // unique on each run. Without this, re-running against the same server (no restart) would reuse
  // deterministic workflow IDs from the previous run and collide.
  private static final String RUN_ID = UUID.randomUUID().toString().substring(0, 8);

  public static void main(String[] args) throws Exception {
    WorkflowClient client = ClientOptions.getWorkflowClient();
    WorkflowServiceStubs stubs = client.getWorkflowServiceStubs();
    String namespace = client.getOptions().getNamespace();

    // A single NexusClient is the entry point: it serves Visibility queries (list/count) and
    // produces service-bound clients.
    NexusClient nexusClient = NexusClient.newInstance(stubs, clientOptions(namespace));
    // Typed service client: dispatches operations by method reference on the service interface.
    NexusServiceClient<GreetingNexusService> greetingClient =
        nexusClient.newNexusServiceClient(GreetingNexusService.class, ENDPOINT_NAME);

    demonstrateExecute(greetingClient);
    demonstrateCancel(greetingClient);
    demonstrateTerminate(greetingClient, client);
    demonstrateVisibility(nexusClient);
    demonstrateClientOptionsAndInterceptors(stubs, namespace);
  }

  // ─────────────────────────────────────────────────────────────────────────────────────────────
  // execute() and executeAsync() — run a standalone Nexus operation and return its result.
  // ─────────────────────────────────────────────────────────────────────────────────────────────
  private static void demonstrateExecute(NexusServiceClient<GreetingNexusService> nexusClient)
      throws Exception {
    // execute(...) starts the operation and blocks until it completes, returning the result in one
    // call (equivalent to start(...).getResult()). Used here on the synchronous 'greet' operation.
    GreetingOutput executed =
        nexusClient.execute(
            GreetingNexusService::greet, new GreetingInput("execute"), basicOptions());
    logger.info("execute() returned: {}", executed.getMessage());

    // executeAsync(...) is the same but returns a CompletableFuture instead of blocking.
    CompletableFuture<GreetingOutput> future =
        nexusClient.executeAsync(
            GreetingNexusService::greet, new GreetingInput("executeAsync"), basicOptions());

    // Call get on the future to block and wait on the result:
    logger.info("executeAsync() returned: {}", future.get().getMessage());
  }

  // ─────────────────────────────────────────────────────────────────────────────────────────────
  // cancel — cooperative for workflow-backed operations (see GreetingWorkflowImpl comment).
  // ─────────────────────────────────────────────────────────────────────────────────────────────
  private static void demonstrateCancel(NexusServiceClient<GreetingNexusService> nexusClient)
      throws Exception {
    // Never signaled, so the backing workflow blocks indefinitely — giving cancellation something
    // to act on.
    NexusOperationHandle<GreetingOutput> handle =
        nexusClient.start(
            GreetingNexusService::startGreeting,
            new GreetingInput("to-cancel-" + RUN_ID),
            basicOptions());
    logger.info("Started 'to-cancel' id={}, requesting cancellation", handle.getNexusOperationId());
    handle.cancel("standalone-nexus sample: cancel demo");
    logger.info(
        "Operation id={} final status: {}",
        handle.getNexusOperationId(),
        awaitTerminalStatus(handle, Duration.ofSeconds(10)));
  }

  // ─────────────────────────────────────────────────────────────────────────────────────────────
  // terminate — forcefully closes the operation record.
  //
  // KNOWN FEATURE GAP: terminating a standalone Nexus operation terminates ONLY the operation
  // record — it does NOT propagate to the backing workflow (unlike cancel, which does). The backing
  // workflow keeps running and nothing appears in its history. Until the server closes this gap,
  // terminate the backing workflow directly by its workflow ID to avoid orphaning it.
  // ─────────────────────────────────────────────────────────────────────────────────────────────
  private static void demonstrateTerminate(
      NexusServiceClient<GreetingNexusService> nexusClient, WorkflowClient client) {
    String name = "to-terminate-" + RUN_ID;
    NexusOperationHandle<GreetingOutput> handle =
        nexusClient.start(
            GreetingNexusService::startGreeting, new GreetingInput(name), basicOptions());
    logger.info("Started 'to-terminate' id={}, terminating", handle.getNexusOperationId());
    handle.terminate("standalone-nexus sample: terminate demo");
    logger.info(
        "Final status of 'to-terminate': {}", awaitTerminalStatus(handle, Duration.ofSeconds(10)));
    // Operation-terminate did not stop the backing workflow (see the gap note above), so terminate
    // it directly by its ID.
    terminateBackingWorkflow(client, name);
  }

  // ─────────────────────────────────────────────────────────────────────────────────────────────
  // Visibility — list (filtered) and count (total and grouped) standalone operations.
  // ─────────────────────────────────────────────────────────────────────────────────────────────
  private static void demonstrateVisibility(NexusClient visibilityClient) {
    // list accepts a Temporal Visibility query to filter results. Here we filter by the built-in
    // ExecutionStatus attribute. Note the value is the SHORT status name ("Completed", "Canceled",
    // "Terminated", "Running", ...) — not the full NEXUS_OPERATION_EXECUTION_STATUS_* enum
    // constant.
    // Visibility query syntax (operators, fields, AND/OR) is documented at
    // https://docs.temporal.io/visibility#list-filter .
    String completedQuery = "ExecutionStatus = \"Completed\"";
    List<NexusOperationExecutionMetadata> completed =
        visibilityClient.listNexusOperationExecutions(completedQuery).collect(Collectors.toList());
    logger.info("List filtered to Completed returned {} operation(s)", completed.size());

    // count() with no query returns the total in the namespace.
    NexusOperationExecutionCount total = visibilityClient.countNexusOperationExecutions(null);
    logger.info("Total operation count: {}", total.getCount());

    // count() with a GROUP BY query returns aggregation groups (a count per group value).
    NexusOperationExecutionCount grouped =
        visibilityClient.countNexusOperationExecutions("GROUP BY ExecutionStatus");
    logger.info("Grouped count total={}, groups:", grouped.getCount());
    for (NexusOperationExecutionCount.AggregationGroup group : grouped.getGroups()) {
      logger.info("  group values={} count={}", group.getGroupValues(), group.getCount());
    }
  }

  // ─────────────────────────────────────────────────────────────────────────────────────────────
  // Client-wide options (identity, data converter) and interceptors.
  // ─────────────────────────────────────────────────────────────────────────────────────────────
  private static void demonstrateClientOptionsAndInterceptors(
      WorkflowServiceStubs stubs, String namespace) throws Exception {
    NexusClientOptions options =
        NexusClientOptions.newBuilder()
            .setNamespace(namespace)
            // identity is stamped on write requests (start/cancel/terminate) for audit trails.
            .setIdentity("standalone-nexus-sample")
            // the data converter (de)serializes operation inputs/results. Supply a custom one for
            // e.g. encryption; here we use the global default explicitly.
            // See https://docs.temporal.io/default-custom-data-converters
            .setDataConverter(GlobalDataConverter.get())
            // interceptors wrap every per-call operation. Registration order matters: the LAST
            // registered interceptor is the OUTERMOST. With [first, second], a single start call
            // enters 'second', then 'first', then the root invoker, and returns back out through
            // 'first' then 'second' — so each interceptor logs once on the way in and once on the
            // way out (four lines total for one operation).
            // See https://docs.temporal.io/encyclopedia/interceptors
            .setInterceptors(
                Arrays.asList(
                    new LoggingNexusClientInterceptor("first"),
                    new LoggingNexusClientInterceptor("second")))
            .build();

    NexusServiceClient<GreetingNexusService> interceptedClient =
        NexusClient.newInstance(stubs, options)
            .newNexusServiceClient(GreetingNexusService.class, ENDPOINT_NAME);
    GreetingOutput out =
        interceptedClient.execute(
            GreetingNexusService::greet, new GreetingInput("interceptors"), basicOptions());
    logger.info("Result through interceptor chain: {}", out.getMessage());
  }

  // ── helpers ──────────────────────────────────────────────────────────────────────────────────

  private static NexusClientOptions clientOptions(String namespace) {
    return NexusClientOptions.newBuilder().setNamespace(namespace).build();
  }

  /** Builds the per-call options used to start a Nexus operation. */
  private static StartNexusOperationOptions basicOptions() {
    return StartNexusOperationOptions.newBuilder()
        // Required: a namespace-unique operation ID. The SDK never generates one for you, so you
        // must supply your own (a UUID here).
        .setId(UUID.randomUUID().toString())
        // Total time the caller is willing to wait for the operation to complete, including any
        // server-side retries. Defaults to none (bounded only by server limits) if not set.
        .setScheduleToCloseTimeout(Duration.ofMinutes(5))
        // Other optional per-call options (not set here, shown for reference):
        //   .setScheduleToStartTimeout(...) — max time the start request may wait before a handler
        //       picks it up. Default: unset (no limit).
        //   .setStartToCloseTimeout(...)    — max time for a single start attempt. Default: unset.
        //   .setTypedSearchAttributes(...)  — Visibility search attributes to index the operation
        //       by; each attribute must be registered on the namespace first. Default: none.
        //   .setSummary(...)                — short text shown in the UI and returned by
        //       describe().getStaticSummary(). Default: none.
        //   .setIdReusePolicy(...)          — behavior when the ID was used by a previously CLOSED
        //       operation. Default: ALLOW_DUPLICATE (a new run may reuse the ID).
        //   .setIdConflictPolicy(...)       — behavior when the ID belongs to a currently RUNNING
        //       operation. Default: FAIL (reject with NexusOperationAlreadyStartedException).
        .build();
  }

  /** Polls describe() until the operation leaves the RUNNING state or the budget elapses. */
  private static NexusOperationExecutionStatus awaitTerminalStatus(
      NexusOperationHandle<?> handle, Duration budget) {
    long deadlineMillis = System.currentTimeMillis() + budget.toMillis();
    NexusOperationExecutionStatus status = handle.describe().getStatus();
    while (status == NexusOperationExecutionStatus.NEXUS_OPERATION_EXECUTION_STATUS_RUNNING
        && System.currentTimeMillis() < deadlineMillis) {
      sleep(Duration.ofMillis(200));
      status = handle.describe().getStatus();
    }
    return status;
  }

  /**
   * Terminates the backing workflow for {@code name} directly by its workflow ID. Needed because
   * terminating a standalone Nexus operation is a known gap that does not propagate to the backing
   * workflow. Best-effort: ignores the case where the workflow is already closed.
   */
  private static void terminateBackingWorkflow(WorkflowClient client, String name) {
    String workflowId = GreetingIds.backingWorkflowId(name);
    try {
      client
          .newUntypedWorkflowStub(workflowId)
          .terminate("standalone-nexus sample: terminate orphaned backing workflow");
      logger.info("Terminated backing workflow {}", workflowId);
    } catch (Exception e) {
      logger.info(
          "Backing workflow {} not terminated (already closed?): {}", workflowId, e.getMessage());
    }
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  // ── interceptors ─────────────────────────────────────────────────────────────────────────────

  /** Outer interceptor: builds the per-call interceptor that logs each start RPC. */
  private static final class LoggingNexusClientInterceptor implements NexusClientInterceptor {
    private final String name;

    LoggingNexusClientInterceptor(String name) {
      this.name = name;
    }

    @Override
    public NexusClientCallsInterceptor nexusClientCallsInterceptor(
        NexusClientCallsInterceptor next) {
      return new LoggingNexusClientCalls(name, next);
    }
  }

  /** Per-call interceptor that logs each start RPC as it passes through the chain. */
  private static final class LoggingNexusClientCalls extends NexusClientCallsInterceptorBase {
    private final String name;

    LoggingNexusClientCalls(String name, NexusClientCallsInterceptor next) {
      super(next);
      this.name = name;
    }

    @Override
    public StartNexusOperationExecutionOutput startNexusOperationExecution(
        StartNexusOperationExecutionInput input) {
      logger.info("[interceptor {}] -> startNexusOperationExecution", name);
      // Delegate to the next interceptor in the chain — and, at the tail of the chain, the SDK's
      // root invoker, which issues the StartNexusOperationExecution gRPC call to the Temporal
      // service. This delegation is REQUIRED: it is what actually starts the operation. An
      // interceptor that returns without calling super short-circuits the chain, so no operation is
      // started.
      return super.startNexusOperationExecution(input);
    }
  }
}
