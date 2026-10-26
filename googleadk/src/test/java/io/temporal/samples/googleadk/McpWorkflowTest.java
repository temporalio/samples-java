package io.temporal.samples.googleadk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.googleadk.testing.ScriptedModel;
import io.temporal.samples.googleadk.mcp.McpWorkflow;
import io.temporal.samples.googleadk.mcp.McpWorkflowImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(30)
class McpWorkflowTest {
  @Test
  void realStdioEchoServerIsListedAndCalledThroughActivities() throws Exception {
    ScriptedModel model =
        ScriptedModel.responding(
            AgentSupport.MODEL,
            request ->
                AdkTestEnvironment.functionResponse(request, "echo")
                    .map(
                        response -> {
                          assertTrue(response.toString().contains("hello MCP"));
                          return ScriptedModel.text("hello MCP");
                        })
                    .orElseGet(
                        () -> {
                          assertTrue(request.tools().containsKey("echo"));
                          return ScriptedModel.functionCall("echo", Map.of("text", "hello MCP"));
                        }));
    try (AdkTestEnvironment test = new AdkTestEnvironment(model, McpWorkflowImpl.class)) {
      test.environment.start();
      McpWorkflow workflow = test.workflow(McpWorkflow.class);

      assertEquals("hello MCP", workflow.echo("hello MCP"));
      assertEquals(
          List.of(
              "googleadk.ListMcpTools",
              "googleadk.InvokeModel",
              "googleadk.CallMcpTool",
              "googleadk.InvokeModel"),
          AdkTestEnvironment.activities(test.history(workflow)));
      test.replay(test.history(workflow), McpWorkflowImpl.class);
    }
  }
}
