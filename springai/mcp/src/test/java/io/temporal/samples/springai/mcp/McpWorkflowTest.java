package io.temporal.samples.springai.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.modelcontextprotocol.spec.McpSchema;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.springai.activity.ChatModelActivityImpl;
import io.temporal.springai.mcp.McpClientActivity;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.WorkflowReplayer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

class McpWorkflowTest {

  @Test
  void discoveredToolsAndConversationMemorySurviveReplay() throws Exception {
    String toolName = McpToolUtils.prefixedToolName("filesystem", "read_file");
    List<Prompt> requests = new CopyOnWriteArrayList<>();
    ChatModel model =
        prompt -> {
          requests.add(prompt);
          AssistantMessage response;
          if (requests.size() == 1) {
            response =
                AssistantMessage.builder()
                    .toolCalls(
                        List.of(
                            new AssistantMessage.ToolCall(
                                "read-1", "function", toolName, "{\"path\":\"/tmp/notes.txt\"}")))
                    .build();
          } else {
            response =
                new AssistantMessage(
                    requests.size() == 2 ? "The file says hello." : "You asked about notes.txt.");
          }
          return new ChatResponse(List.of(new Generation(response)));
        };
    FakeMcpClientActivity mcp = new FakeMcpClientActivity();

    try (TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance()) {
      var worker = env.newWorker("spring-ai-mcp-test");
      worker.registerWorkflowImplementationTypes(McpWorkflowImpl.class);
      worker.registerActivitiesImplementations(new ChatModelActivityImpl(model), mcp);
      env.start();

      McpWorkflow workflow =
          env.getWorkflowClient()
              .newWorkflowStub(
                  McpWorkflow.class,
                  WorkflowOptions.newBuilder()
                      .setTaskQueue("spring-ai-mcp-test")
                      .setWorkflowExecutionTimeout(Duration.ofSeconds(30))
                      .build());
      WorkflowClient.start(workflow::run);
      // The signal may arrive while tool discovery is still initializing.
      workflow.chat("Read /tmp/notes.txt.");
      awaitResponse(workflow, "The file says hello.");
      assertEquals("Available MCP tools:\n - " + toolName + ": Read a file", workflow.listTools());
      workflow.chat("Which file did I ask about?");
      awaitResponse(workflow, "You asked about notes.txt.");
      workflow.end();
      WorkflowStub stub = WorkflowStub.fromTyped(workflow);
      assertEquals("Chat ended after 2 messages.", stub.getResult(String.class));

      assertEquals(3, requests.size());
      var options = assertInstanceOf(ToolCallingChatOptions.class, requests.get(0).getOptions());
      assertEquals(1, options.getToolCallbacks().size());
      assertEquals(toolName, options.getToolCallbacks().get(0).getToolDefinition().name());
      assertEquals(List.of("filesystem"), mcp.toolClients);
      assertEquals(1, mcp.toolRequests.size());
      assertEquals("read_file", mcp.toolRequests.get(0).name());
      assertEquals(Map.of("path", "/tmp/notes.txt"), mcp.toolRequests.get(0).arguments());
      assertTrue(
          requests.get(1).getInstructions().stream()
              .filter(ToolResponseMessage.class::isInstance)
              .map(ToolResponseMessage.class::cast)
              .flatMap(message -> message.getResponses().stream())
              .anyMatch(response -> response.responseData().contains("hello")));
      assertTrue(
          requests.get(2).getInstructions().stream()
              .anyMatch(message -> "Read /tmp/notes.txt.".equals(message.getText())));
      assertTrue(
          requests.get(2).getInstructions().stream()
              .anyMatch(message -> "The file says hello.".equals(message.getText())));

      WorkflowReplayer.replayWorkflowExecution(
          env.getWorkflowClient().fetchHistory(stub.getExecution().getWorkflowId()),
          McpWorkflowImpl.class);
      assertEquals(3, requests.size());
      assertEquals(1, mcp.toolRequests.size());
    }
  }

  private static void awaitResponse(McpWorkflow workflow, String expected)
      throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    while (!expected.equals(workflow.getLastResponse()) && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }
    assertEquals(expected, workflow.getLastResponse());
  }

  public static class FakeMcpClientActivity implements McpClientActivity {
    final List<String> toolClients = new CopyOnWriteArrayList<>();
    final List<McpSchema.CallToolRequest> toolRequests = new CopyOnWriteArrayList<>();

    @Override
    public Map<String, McpSchema.ServerCapabilities> getServerCapabilities() {
      return Map.of();
    }

    @Override
    public Map<String, McpSchema.Implementation> getClientInfo() {
      return Map.of("filesystem", McpSchema.Implementation.builder("filesystem", "1.0").build());
    }

    @Override
    public Map<String, McpSchema.ListToolsResult> listTools() {
      McpSchema.Tool tool =
          McpSchema.Tool.builder(
                  "read_file",
                  Map.of(
                      "type", "object",
                      "properties", Map.of("path", Map.of("type", "string")),
                      "required", List.of("path")))
              .description("Read a file")
              .build();
      return Map.of("filesystem", McpSchema.ListToolsResult.builder(List.of(tool)).build());
    }

    @Override
    public McpSchema.CallToolResult callTool(String clientName, McpSchema.CallToolRequest request) {
      toolClients.add(clientName);
      toolRequests.add(request);
      return McpSchema.CallToolResult.builder().addTextContent("hello").isError(false).build();
    }
  }
}
