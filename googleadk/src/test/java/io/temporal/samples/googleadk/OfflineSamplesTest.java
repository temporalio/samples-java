package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.samples.googleadk.chat.ChatInput;
import io.temporal.samples.googleadk.chat.ChatWorkflow;
import io.temporal.samples.googleadk.humanintheloop.ApprovalWorkflow;
import io.temporal.samples.googleadk.longrunning.LongRunningWorkflow;
import io.temporal.samples.googleadk.mcp.McpWorkflow;
import io.temporal.samples.googleadk.multiagent.MultiAgentWorkflow;
import io.temporal.samples.googleadk.structuredoutput.StructuredOutputWorkflow;
import io.temporal.samples.googleadk.structuredoutput.TravelPlan;
import io.temporal.samples.googleadk.tools.ToolsWorkflow;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(60)
class OfflineSamplesTest {
  @Test
  void everyScenarioRunsWithTheProductionWorkerRegistrationAndOfflineProvider() throws Exception {
    try (AdkTestEnvironment test = new AdkTestEnvironment(new OfflineModel())) {
      SampleWorker.register(test.worker);
      test.environment.start();
      assertEquals(
          "Chicago is sunny: 20 C / 68.0 F.",
          test.workflow(ToolsWorkflow.class).ask("What's the weather in Chicago?"));
      assertEquals(
          List.of("Bring a warm jacket.", "Visit Chicago's lakefront."),
          test.workflow(MultiAgentWorkflow.class).plan("Chicago"));
      assertEquals("Hello MCP", test.workflow(McpWorkflow.class).echo("Hello MCP"));
      assertEquals(
          new TravelPlan("Chicago", 2, List.of("Walk the lakefront", "Visit a museum")),
          test.workflow(StructuredOutputWorkflow.class).plan("Plan two days in Chicago."));
      assertEquals(
          "turns=2; latest: two",
          test.workflow(ChatWorkflow.class)
              .chat(new ChatInput(null, List.of("one", "two"), true, "")));

      ApprovalWorkflow approval = test.workflow(ApprovalWorkflow.class);
      WorkflowClient.start(approval::fulfill, "Ship demo-order.");
      AdkTestEnvironment.await(() -> !approval.pendingConfirmations().isEmpty());
      approval.decide(true);
      assertEquals("Order shipped.", WorkflowStub.fromTyped(approval).getResult(String.class));

      LongRunningWorkflow report = test.workflow(LongRunningWorkflow.class);
      WorkflowClient.start(report::prepare, "Prepare a sales report.");
      AdkTestEnvironment.await(() -> !report.pendingCall().isEmpty());
      report.complete(report.pendingCall(), "Revenue grew 10%.");
      assertEquals(
          "Report complete: Revenue grew 10%.",
          WorkflowStub.fromTyped(report).getResult(String.class));
    }
  }
}
