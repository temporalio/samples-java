package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.api.enums.v1.EventType;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.googleadk.testing.ScriptedModel;
import io.temporal.samples.googleadk.multiagent.MultiAgentWorkflow;
import io.temporal.samples.googleadk.multiagent.MultiAgentWorkflowImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(30)
class MultiAgentWorkflowTest {
  @Test
  void bothAgentsScheduleTheirModelActivitiesTogetherAndReplay() throws Exception {
    ScriptedModel model =
        ScriptedModel.responding(
            AgentSupport.MODEL,
            request ->
                ScriptedModel.text(
                    request.getSystemInstructions().stream()
                            .anyMatch(text -> text.contains("\"planner\""))
                        ? "Visit the lakefront."
                        : "Bring a warm jacket."));
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, MultiAgentWorkflowImpl.class)) {
      test.environment.start();
      MultiAgentWorkflow workflow = test.workflow(MultiAgentWorkflow.class);

      assertEquals(
          List.of("Bring a warm jacket.", "Visit the lakefront."), workflow.plan("Chicago"));
      WorkflowExecutionHistory history = test.history(workflow);
      List<EventType> activityOrder =
          history.getEvents().stream()
              .map(HistoryEvent::getEventType)
              .filter(
                  type ->
                      type == EventType.EVENT_TYPE_ACTIVITY_TASK_SCHEDULED
                          || type == EventType.EVENT_TYPE_ACTIVITY_TASK_COMPLETED)
              .toList();
      assertEquals(
          List.of(
              EventType.EVENT_TYPE_ACTIVITY_TASK_SCHEDULED,
              EventType.EVENT_TYPE_ACTIVITY_TASK_SCHEDULED),
          activityOrder.subList(0, 2));
      test.replay(history, MultiAgentWorkflowImpl.class);
    }
  }
}
