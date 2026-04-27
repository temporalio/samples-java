package io.temporal.samples.springai.mcp;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Example application demonstrating MCP (Model Context Protocol) integration.
 *
 * <p>This application shows how to use tools from MCP servers within Temporal workflows. It
 * connects to a filesystem MCP server and provides an AI assistant that can read and write files.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * Commands:
 *   tools                - List available MCP tools
 *   &lt;any message&gt;       - Chat with the AI (it can use file tools)
 *   quit                 - End the chat
 * </pre>
 *
 * <h2>Example Interactions</h2>
 *
 * <pre>
 * &gt; List files in the current directory
 * [AI uses list_directory tool and returns results]
 *
 * &gt; Create a file called hello.txt with "Hello from MCP!"
 * [AI uses write_file tool]
 *
 * &gt; Read the contents of hello.txt
 * [AI uses read_file tool]
 * </pre>
 *
 * <h2>Prerequisites</h2>
 *
 * <ol>
 *   <li>Start a Temporal dev server: {@code temporal server start-dev}
 *   <li>Set OPENAI_API_KEY environment variable
 *   <li>Ensure Node.js/npx is available (for MCP server)
 *   <li>Optionally set MCP_ALLOWED_PATH (defaults to /tmp/mcp-example)
 *   <li>Run: {@code ./gradlew :example-mcp:bootRun}
 * </ol>
 */
@SpringBootApplication
public class McpApplication {

  private static final String TASK_QUEUE = "mcp-example-queue";

  @Autowired private WorkflowClient workflowClient;

  public static void main(String[] args) throws Exception {
    // The filesystem MCP server refuses to start if the allowed path doesn't
    // exist, so create it up front. Must happen before SpringApplication.run —
    // the MCP client connects during context startup.
    String allowedPath = System.getenv().getOrDefault("MCP_ALLOWED_PATH", "/tmp/mcp-example");
    Files.createDirectories(Path.of(allowedPath));

    SpringApplication.run(McpApplication.class, args);
  }

  /** Runs after workers are started (ApplicationReadyEvent fires after CommandLineRunner). */
  @EventListener(ApplicationReadyEvent.class)
  public void onReady() throws Exception {
    // Start a new workflow
    String workflowId = "mcp-example-" + UUID.randomUUID().toString().substring(0, 8);
    McpWorkflow workflow =
        workflowClient.newWorkflowStub(
            McpWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId(workflowId)
                .build());

    // Start the workflow asynchronously
    WorkflowClient.start(workflow::run);

    // Give the workflow time to initialize (first workflow task must complete)
    Thread.sleep(1000);

    System.out.println("\n=== MCP Tools Demo ===");
    System.out.println("Workflow ID: " + workflowId);
    System.out.println("\nThis demo uses the filesystem MCP server.");
    System.out.println("The AI can read, write, and list files in the allowed directory.");
    System.out.println("\nCommands:");
    System.out.println("  tools    - List available MCP tools");
    System.out.println("  <text>   - Chat with the AI");
    System.out.println("  quit     - End the chat");
    System.out.println();

    // Get a workflow stub for sending signals/queries
    McpWorkflow workflowStub = workflowClient.newWorkflowStub(McpWorkflow.class, workflowId);

    // Note: tools command may take a moment to work while workflow initializes
    System.out.println("Type 'tools' to list available MCP tools.\n");

    Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);
    while (true) {
      System.out.print("> ");
      String input = scanner.nextLine().trim();

      if (input.equalsIgnoreCase("quit")) {
        workflowStub.end();
        System.out.println("Chat ended. Goodbye!");
        break;
      }

      if (input.equalsIgnoreCase("tools")) {
        System.out.println(workflowStub.listTools());
        continue;
      }

      if (input.isEmpty()) {
        continue;
      }

      System.out.println("[Processing...]");

      // Capture current response BEFORE sending, so we can detect when it changes
      String previousResponse = workflowStub.getLastResponse();

      // Send the message via signal
      workflowStub.chat(input);

      // Poll until the response changes (workflow has processed our message)
      for (int i = 0; i < 600; i++) { // Wait up to 60 seconds (MCP tools can be slow)
        String response = workflowStub.getLastResponse();
        if (!response.equals(previousResponse)) {
          System.out.println("\n[AI]: " + response + "\n");
          break;
        }
        Thread.sleep(100);
      }
    }
  }
}
