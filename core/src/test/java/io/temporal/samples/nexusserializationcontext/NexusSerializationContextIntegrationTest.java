package io.temporal.samples.nexusserializationcontext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.api.common.v1.Payload;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.history.v1.WorkflowExecutionStartedEventAttributes;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.samples.nexusserializationcontext.caller.CallerWorker;
import io.temporal.samples.nexusserializationcontext.caller.CallerWorkflow;
import io.temporal.samples.nexusserializationcontext.caller.CallerWorkflowImpl;
import io.temporal.samples.nexusserializationcontext.caller.EndpointResults;
import io.temporal.samples.nexusserializationcontext.handler.AsyncEchoServiceImpl;
import io.temporal.samples.nexusserializationcontext.handler.CompressedEncryptedHandlerWorker;
import io.temporal.samples.nexusserializationcontext.handler.EchoWorkflowImpl;
import io.temporal.samples.nexusserializationcontext.handler.EncryptedHandlerWorker;
import io.temporal.samples.nexusserializationcontext.handler.SyncEchoServiceImpl;
import io.temporal.samples.nexusserializationcontext.service.AsyncEchoService;
import io.temporal.testing.TemporalDevServerOptions;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class NexusSerializationContextIntegrationTest {
  private static final String DEV_SERVER_VERSION = "v1.9.3-nexus-serialization-context";

  @Test
  @Timeout(120)
  void asyncResultsUseTheEndpointContextRecordedByTheServer() throws InterruptedException {
    try (TestWorkflowEnvironment environment =
        TestWorkflowEnvironment.startLocal(
            TestEnvironmentOptions.newBuilder()
                .setWorkflowClientOptions(
                    WorkflowClientOptions.newBuilder()
                        .setDataConverter(CallerWorker.dataConverter())
                        .build())
                .build(),
            TemporalDevServerOptions.newBuilder().setDownloadVersion(DEV_SERVER_VERSION).build())) {
      String callerQueue = "serialization-caller";
      String handlerAQueue = "serialization-handler-a";
      String handlerBQueue = "serialization-handler-b";
      WorkerFactory handlerA =
          handlerFactory(
              environment, handlerAQueue, CompressedEncryptedHandlerWorker.dataConverter());
      WorkerFactory handlerB =
          handlerFactory(environment, handlerBQueue, EncryptedHandlerWorker.dataConverter());
      try {
        environment.createNexusEndpoint(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT, handlerAQueue);
        environment.createNexusEndpoint(SampleConfig.ENCRYPTED_ENDPOINT, handlerBQueue);
        environment
            .newWorker(callerQueue)
            .registerWorkflowImplementationTypes(CallerWorkflowImpl.class);
        handlerA.start();
        handlerB.start();
        environment.start();

        WorkflowClient client = environment.getWorkflowClient();
        CallerWorkflow caller =
            client.newWorkflowStub(
                CallerWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setTaskQueue(callerQueue)
                    .setWorkflowExecutionTimeout(Duration.ofSeconds(60))
                    .build());
        String message = "Hello from Nexus";
        assertEquals(
            new EndpointResults(message, message, message, message),
            caller.echoThroughEndpoints(message));

        Map<String, String> expectedKeys =
            Map.of(
                SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT, SampleConfig.KEY_A_ID,
                SampleConfig.ENCRYPTED_ENDPOINT, SampleConfig.KEY_B_ID);
        Set<String> observedEndpoints = new HashSet<>();
        for (WorkflowExecution execution : backingWorkflows(environment)) {
          WorkflowExecutionHistory history =
              client.fetchHistory(execution.getWorkflowId(), execution.getRunId());
          WorkflowExecutionStartedEventAttributes started =
              history.getEvents().get(0).getWorkflowExecutionStartedEventAttributes();
          assertTrue(started.hasPropagatedNexusSerializationContext());
          String endpoint = started.getPropagatedNexusSerializationContext().getEndpoint();
          assertTrue(expectedKeys.containsKey(endpoint), "Unexpected endpoint: " + endpoint);
          assertTrue(observedEndpoints.add(endpoint), "Duplicate endpoint: " + endpoint);
          assertEquals(
              AsyncEchoService.SERVICE_NAME,
              started.getPropagatedNexusSerializationContext().getService());
          assertEquals(
              AsyncEchoService.ECHO_ASYNC_OPERATION_NAME,
              started.getPropagatedNexusSerializationContext().getOperation());

          // Native propagation uses the start-event field, not the sample's old custom header.
          assertFalse(started.getHeader().containsFields(SampleConfig.ENDPOINT_METADATA_KEY));
          assertFalse(
              started
                  .getInput()
                  .getPayloads(0)
                  .containsMetadata(SampleConfig.ENDPOINT_METADATA_KEY));
          assertTrue(history.getLastEvent().hasWorkflowExecutionCompletedEventAttributes());
          Payload result =
              history
                  .getLastEvent()
                  .getWorkflowExecutionCompletedEventAttributes()
                  .getResult()
                  .getPayloads(0);
          assertEquals(
              "binary/nexus-aes-gcm",
              result.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8());
          assertEquals(
              endpoint,
              result.getMetadataOrThrow(SampleConfig.ENDPOINT_METADATA_KEY).toStringUtf8());
          assertEquals(
              expectedKeys.get(endpoint),
              result.getMetadataOrThrow("encryption-key-id").toStringUtf8());
        }
        assertEquals(expectedKeys.keySet(), observedEndpoints);
      } finally {
        handlerA.shutdownNow();
        handlerB.shutdownNow();
        handlerA.awaitTermination(10, TimeUnit.SECONDS);
        handlerB.awaitTermination(10, TimeUnit.SECONDS);
      }
    }
  }

  private static WorkerFactory handlerFactory(
      TestWorkflowEnvironment environment, String taskQueue, DataConverter converter) {
    WorkflowClient client =
        WorkflowClient.newInstance(
            environment.getWorkflowServiceStubs(),
            WorkflowClientOptions.newBuilder()
                .setNamespace(environment.getNamespace())
                .setDataConverter(converter)
                .build());
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(taskQueue);
    worker.registerWorkflowImplementationTypes(EchoWorkflowImpl.class);
    worker.registerNexusServiceImplementation(
        new SyncEchoServiceImpl(), new AsyncEchoServiceImpl());
    return factory;
  }

  private static List<WorkflowExecution> backingWorkflows(TestWorkflowEnvironment environment) {
    // Visibility is eventually consistent; wait for both backing workflows to be indexed.
    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    do {
      List<WorkflowExecution> executions;
      try (var stream =
          environment.getWorkflowClient().listExecutions("WorkflowType = 'EchoWorkflow'")) {
        executions = stream.map(metadata -> metadata.getExecution()).collect(Collectors.toList());
      }
      if (executions.size() == 2) {
        return executions;
      }
      environment.sleep(Duration.ofMillis(100));
    } while (System.nanoTime() < deadline);
    throw new AssertionError("Expected two backing workflows to appear in visibility");
  }
}
