package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.LlmRequest;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.googleadk.GoogleAdkPlugin;
import io.temporal.googleadk.testing.ScriptedModel;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Tests use the published scripted model and an in-process Temporal Service. */
final class AdkTestEnvironment implements AutoCloseable {
  final TestWorkflowEnvironment environment;
  final WorkflowClient client;
  final Worker worker;
  final String taskQueue = "google-adk-test-" + UUID.randomUUID();
  private final BaseLlm model;

  AdkTestEnvironment(BaseLlm model, Class<?>... workflows) {
    this.model = model;
    GoogleAdkPlugin plugin = SampleWorker.plugin(() -> model);
    environment = newEnvironment(plugin);
    client = environment.getWorkflowClient();
    worker = environment.newWorker(taskQueue);
    if (workflows.length > 0) {
      worker.registerWorkflowImplementationTypes(workflows);
    }
  }

  private static TestWorkflowEnvironment newEnvironment(GoogleAdkPlugin plugin) {
    return TestWorkflowEnvironment.newInstance(
        TestEnvironmentOptions.newBuilder()
            .setWorkflowClientOptions(WorkflowClientOptions.newBuilder().setPlugins(plugin).build())
            .build());
  }

  <T> T workflow(Class<T> type) {
    return client.newWorkflowStub(
        type,
        WorkflowOptions.newBuilder()
            .setTaskQueue(taskQueue)
            .setWorkflowId("google-adk-test-" + UUID.randomUUID())
            .setWorkflowExecutionTimeout(Duration.ofMinutes(2))
            .build());
  }

  WorkflowExecutionHistory history(Object workflow) {
    return client.fetchHistory(WorkflowStub.fromTyped(workflow).getExecution().getWorkflowId());
  }

  void replay(WorkflowExecutionHistory history, Class<?> implementation) throws Exception {
    int requests = model instanceof ScriptedModel scripted ? scripted.requests().size() : 0;
    // A new Worker proves replay works without cached Workflow or ADK session state.
    GoogleAdkPlugin plugin = GoogleAdkPlugin.newBuilder().model(AgentSupport.MODEL, model).build();
    try (TestWorkflowEnvironment replay = newEnvironment(plugin)) {
      Worker replayWorker = replay.newWorker("replay");
      replayWorker.registerWorkflowImplementationTypes(implementation);
      replayWorker.replayWorkflowExecution(history);
    }
    if (model instanceof ScriptedModel scripted) {
      assertEquals(requests, scripted.requests().size(), "replay must not call the model again");
    }
  }

  static List<String> activities(WorkflowExecutionHistory history) {
    return history.getEvents().stream()
        .filter(HistoryEvent::hasActivityTaskScheduledEventAttributes)
        .map(event -> event.getActivityTaskScheduledEventAttributes().getActivityType().getName())
        .toList();
  }

  static Optional<Map<String, Object>> functionResponse(LlmRequest request, String name) {
    return OfflineModel.functionResponse(request, name);
  }

  static void await(BooleanSupplier ready) throws InterruptedException {
    for (int i = 0; i < 1000 && !ready.getAsBoolean(); i++) {
      Thread.sleep(10);
    }
    assertTrue(ready.getAsBoolean(), "the Workflow did not reach the expected state");
  }

  @Override
  public void close() {
    environment.close();
  }
}
