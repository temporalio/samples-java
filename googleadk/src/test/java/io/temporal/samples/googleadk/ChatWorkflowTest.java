package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.EventType;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.googleadk.testing.ScriptedModel;
import io.temporal.samples.googleadk.chat.ChatInput;
import io.temporal.samples.googleadk.chat.ChatWorkflow;
import io.temporal.samples.googleadk.chat.ChatWorkflowImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(30)
class ChatWorkflowTest {
  private static ScriptedModel countingModel() {
    return ScriptedModel.responding(
        AgentSupport.MODEL,
        request ->
            ScriptedModel.text(
                "turns="
                    + request.contents().stream()
                        .filter(content -> content.role().orElse("").equals("user"))
                        .count()));
  }

  @Test
  void conversationAndQueuedSignalsSurviveContinueAsNew() throws Exception {
    ScriptedModel model = countingModel();
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, ChatWorkflowImpl.class)) {
      test.environment.start();
      ChatWorkflow workflow = test.workflow(ChatWorkflow.class);
      WorkflowExecution firstRun =
          WorkflowClient.start(
              workflow::chat,
              new ChatInput(null, List.of("one", "two", "three", "four"), false, ""));
      // Synchronize on the Run transition before querying the next Run, so Queries
      // cannot race the test service's Continue-As-New command.
      AdkTestEnvironment.await(
          () ->
              test
                  .client
                  .fetchHistory(firstRun.getWorkflowId(), firstRun.getRunId())
                  .getEvents()
                  .stream()
                  .anyMatch(
                      event ->
                          event.getEventType()
                              == EventType.EVENT_TYPE_WORKFLOW_EXECUTION_CONTINUED_AS_NEW));
      AdkTestEnvironment.await(() -> workflow.lastReply().equals("turns=4"));
      workflow.message("five");
      AdkTestEnvironment.await(() -> workflow.lastReply().equals("turns=5"));
      workflow.finish();

      assertEquals("turns=5", WorkflowStub.fromTyped(workflow).getResult(String.class));
      assertEquals(5, model.requests().size());
      WorkflowExecutionHistory firstHistory =
          test.client.fetchHistory(firstRun.getWorkflowId(), firstRun.getRunId());
      assertTrue(
          firstHistory.getEvents().stream()
              .anyMatch(
                  event ->
                      event.getEventType()
                          == EventType.EVENT_TYPE_WORKFLOW_EXECUTION_CONTINUED_AS_NEW));
      WorkflowExecutionHistory lastHistory = test.history(workflow);
      assertEquals(
          firstRun.getRunId(),
          lastHistory
              .getEvents()
              .get(0)
              .getWorkflowExecutionStartedEventAttributes()
              .getContinuedExecutionRunId());
      test.replay(firstHistory, ChatWorkflowImpl.class);
      test.replay(lastHistory, ChatWorkflowImpl.class);
    }
  }

  @Test
  void finishDrainsThePendingMessages() throws Exception {
    // @@@SNIPSTART java-google-adk-chat-test
    ScriptedModel model =
        ScriptedModel.of(
            AgentSupport.MODEL,
            ScriptedModel.text("First reply"),
            ScriptedModel.text("Last reply"));
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, ChatWorkflowImpl.class)) {
      test.environment.start();
      ChatWorkflow workflow = test.workflow(ChatWorkflow.class);
      assertEquals(
          "Last reply", workflow.chat(new ChatInput(null, List.of("one", "two"), true, "")));
      assertEquals(2, model.requests().size());
      test.replay(test.history(workflow), ChatWorkflowImpl.class);
    }
    // @@@SNIPEND
  }
}
