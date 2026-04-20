package io.temporal.samples.springai.mcp;

import io.temporal.springai.chat.TemporalChatClient;
import io.temporal.springai.mcp.ActivityMcpClient;
import io.temporal.springai.mcp.McpToolCallback;
import io.temporal.springai.model.ActivityChatModel;
import io.temporal.workflow.Workflow;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallback;

/**
 * Implementation of the MCP workflow.
 *
 * <p>This demonstrates how to use MCP tools from external servers within a Temporal workflow. The
 * workflow:
 *
 * <ol>
 *   <li>Creates an MCP client that wraps the McpClientActivity
 *   <li>Discovers tools from connected MCP servers
 *   <li>Registers those tools with the chat client
 *   <li>When the AI calls an MCP tool, it executes as a durable activity
 * </ol>
 *
 * <p>This example uses the filesystem MCP server which provides tools like:
 *
 * <ul>
 *   <li>{@code read_file} - Read contents of a file
 *   <li>{@code write_file} - Write content to a file
 *   <li>{@code list_directory} - List directory contents
 *   <li>{@code create_directory} - Create a new directory
 * </ul>
 */
public class McpWorkflowImpl implements McpWorkflow {

  private ChatClient chatClient;
  private List<ToolCallback> mcpTools;
  private String lastResponse = "";
  private boolean ended = false;
  private int messageCount = 0;
  private boolean initialized = false;

  @Override
  public String run() {
    // Discover MCP tools at the start of workflow execution (not in constructor)
    // This avoids the "root workflow thread yielding" warning
    ActivityMcpClient mcpClient = ActivityMcpClient.create();
    mcpTools = McpToolCallback.fromMcpClient(mcpClient);

    // Create the chat model (uses the default ChatModel bean)
    ActivityChatModel chatModel = ActivityChatModel.forDefault();

    // Create chat memory - uses in-memory storage that gets rebuilt on replay
    // since the same messages will be added in the same order
    ChatMemory chatMemory =
        MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(20)
            .build();

    // Build the chat client with MCP tools and memory advisor
    this.chatClient =
        TemporalChatClient.builder(chatModel)
            .defaultSystem(
                """
                You are a helpful assistant with access to file system tools.
                You can read files, write files, list directories, and more.

                When asked to perform file operations, use your tools.
                Always confirm what you did after completing an operation.
                """)
            .defaultToolCallbacks(mcpTools)
            .defaultAdvisors(PromptChatMemoryAdvisor.builder(chatMemory).build())
            .build();

    initialized = true;

    // Wait until the chat is ended
    Workflow.await(() -> ended);
    return "Chat ended after " + messageCount + " messages.";
  }

  @Override
  public void chat(String message) {
    if (!initialized) {
      lastResponse = "Workflow is still initializing. Please wait a moment.";
      return;
    }

    messageCount++;

    // PromptChatMemoryAdvisor automatically handles conversation history
    lastResponse = chatClient.prompt().user(message).call().content();
  }

  @Override
  public String getLastResponse() {
    return lastResponse;
  }

  @Override
  public String listTools() {
    if (!initialized || mcpTools == null) {
      return "Workflow is still initializing. Please wait a moment and try again.";
    }

    if (mcpTools.isEmpty()) {
      return "No MCP tools available. Check MCP server configuration.";
    }

    return mcpTools.stream()
        .map(
            tool ->
                " - "
                    + tool.getToolDefinition().name()
                    + ": "
                    + tool.getToolDefinition().description())
        .collect(Collectors.joining("\n", "Available MCP tools:\n", ""));
  }

  @Override
  public void end() {
    ended = true;
  }
}
