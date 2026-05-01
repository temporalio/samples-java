package io.temporal.samples.springai.multimodel;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import java.util.Scanner;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example application demonstrating multi-model support with different AI providers.
 *
 * <p>This application shows how to use different AI providers (OpenAI and Anthropic) within the
 * same Temporal workflow. It provides an interactive CLI where you can send messages to different
 * models.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * Commands:
 *   openai: &lt;message&gt;    - Send to OpenAI (gpt-4o-mini)
 *   anthropic: &lt;message&gt; - Send to Anthropic (Claude)
 *   think: &lt;message&gt;     - Send to Anthropic with extended thinking enabled
 *   &lt;message&gt;            - No prefix routes to the default model (the @Primary bean)
 *   quit                  - End the chat
 * </pre>
 *
 * <h2>Prerequisites</h2>
 *
 * <ol>
 *   <li>Start a Temporal dev server: {@code temporal server start-dev}
 *   <li>Set OPENAI_API_KEY environment variable
 *   <li>Set ANTHROPIC_API_KEY environment variable
 *   <li>Run: {@code ./gradlew :example-multi-model:bootRun}
 * </ol>
 */
@SpringBootApplication
public class MultiModelApplication implements CommandLineRunner {

  private static final String TASK_QUEUE = "multi-model-queue";

  @Autowired private WorkflowClient workflowClient;

  public static void main(String[] args) {
    SpringApplication.run(MultiModelApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    // Start a new workflow
    String workflowId = "multi-model-" + UUID.randomUUID().toString().substring(0, 8);
    MultiModelWorkflow workflow =
        workflowClient.newWorkflowStub(
            MultiModelWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId(workflowId)
                .build());

    // Start the workflow asynchronously
    WorkflowClient.start(workflow::run);

    System.out.println("\n=== Multi-Provider Chat Demo ===");
    System.out.println("Workflow ID: " + workflowId);
    System.out.println("\nAvailable models:");
    System.out.println("  openai:    OpenAI gpt-4o-mini");
    System.out.println("  anthropic: Anthropic Claude");
    System.out.println("  think:     Anthropic Claude with extended thinking (per-call options)");
    System.out.println("  default:   no prefix — routes to the @Primary model (OpenAI)");
    System.out.println("\nCommands:");
    System.out.println("  openai: <message>    - Send to OpenAI");
    System.out.println("  anthropic: <message> - Send to Anthropic");
    System.out.println("  think: <message>     - Send to Anthropic with extended thinking enabled");
    System.out.println("  <message>            - No prefix routes to the default model");
    System.out.println("  quit                 - End the chat");
    System.out.println();

    // Get a workflow stub for sending signals
    MultiModelWorkflow workflowStub =
        workflowClient.newWorkflowStub(MultiModelWorkflow.class, workflowId);

    Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);
    while (true) {
      System.out.print("> ");
      String input = scanner.nextLine().trim();

      if (input.equalsIgnoreCase("quit")) {
        workflowStub.end();
        System.out.println("Chat ended. Goodbye!");
        break;
      }

      // Parse the model and message
      String modelName;
      String message;

      if (input.startsWith("openai:")) {
        modelName = "openai";
        message = input.substring(7).trim();
      } else if (input.startsWith("anthropic:")) {
        modelName = "anthropic";
        message = input.substring(10).trim();
      } else if (input.startsWith("think:")) {
        modelName = "think";
        message = input.substring(6).trim();
      } else {
        // No prefix — route to the workflow's "default" model (the @Primary bean,
        // resolved by ActivityChatModel.forDefault() inside the workflow).
        modelName = "default";
        message = input;
      }

      if (message.isEmpty()) {
        System.out.println("Please enter a message.");
        continue;
      }

      System.out.println("[Sending to " + modelName + " model...]");

      // Send the message via signal
      workflowStub.chat(modelName, message);

      // Wait a moment for processing, then query for response
      Thread.sleep(100);

      // Poll for response (in production, you'd use a more sophisticated approach)
      String lastResponse = "";
      for (int i = 0; i < 300; i++) { // Wait up to 30 seconds
        String response = workflowStub.getLastResponse();
        if (!response.isEmpty() && !response.equals(lastResponse)) {
          System.out.println(
              "\n[" + modelName.toUpperCase(java.util.Locale.ROOT) + "]: " + response + "\n");
          break;
        }
        Thread.sleep(100);
      }
    }
  }
}
