package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.adk.models.LlmRequest;
import com.google.genai.types.FunctionResponse;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.googleadk.GoogleAdkPlugin;
import io.temporal.googleadk.testing.ScriptedModel;
import io.temporal.samples.googleadk.mcp.EchoMcpServer;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/** Tests use the published scripted model and an in-process Temporal Service. */
final class AdkTestEnvironment implements AutoCloseable {
  final TestWorkflowEnvironment environment;
  final WorkflowClient client;
  final Worker worker;
  private final ScriptedModel model;

  AdkTestEnvironment(ScriptedModel model, Class<?>... workflows) {
    this.model = model;
    GoogleAdkPlugin plugin =
        GoogleAdkPlugin.newBuilder()
            .model(AgentSupport.MODEL, model)
            .mcpToolset("echo", EchoMcpServer::toolset)
            .build();
    environment = newEnvironment(plugin);
    client = environment.getWorkflowClient();
    worker = environment.newWorker(AgentSupport.TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(workflows);
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
            .setTaskQueue(AgentSupport.TASK_QUEUE)
            .setWorkflowExecutionTimeout(Duration.ofMinutes(2))
            .build());
  }

  WorkflowExecutionHistory history(Object workflow) {
    return client.fetchHistory(WorkflowStub.fromTyped(workflow).getExecution().getWorkflowId());
  }

  void replay(WorkflowExecutionHistory history, Class<?> implementation) throws Exception {
    int requests = model.requests().size();
    // A new Worker proves replay works without cached Workflow or ADK session state.
    GoogleAdkPlugin plugin = GoogleAdkPlugin.newBuilder().model(AgentSupport.MODEL, model).build();
    try (TestWorkflowEnvironment replay = newEnvironment(plugin)) {
      Worker replayWorker = replay.newWorker("replay");
      replayWorker.registerWorkflowImplementationTypes(implementation);
      replayWorker.replayWorkflowExecution(history);
    }
    assertEquals(requests, model.requests().size(), "replay must not call the model again");
  }

  static List<String> activities(WorkflowExecutionHistory history) {
    return history.getEvents().stream()
        .filter(HistoryEvent::hasActivityTaskScheduledEventAttributes)
        .map(event -> event.getActivityTaskScheduledEventAttributes().getActivityType().getName())
        .toList();
  }

  static Optional<Map<String, Object>> functionResponse(LlmRequest request, String name) {
    return request.contents().stream()
        .flatMap(content -> content.parts().orElse(List.of()).stream())
        .flatMap(part -> part.functionResponse().stream())
        .filter(response -> response.name().orElse("").equals(name))
        .map(FunctionResponse::response)
        .flatMap(Optional::stream)
        .reduce((first, last) -> last);
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
