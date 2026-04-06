package io.temporal.samples.springai.sandboxing;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import java.util.Scanner;
import java.util.UUID;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Example application demonstrating sandboxing of unsafe tools.
 *
 * <p>This example shows how to use {@link io.temporal.springai.advisor.SandboxingAdvisor} to safely
 * use tools that are not properly annotated with {@code @DeterministicTool} or
 * {@code @SideEffectTool}.
 *
 * <h2>Running the Example</h2>
 *
 * <pre>
 * # Start Temporal server
 * temporal server start-dev
 *
 * # Set OpenAI API key
 * export OPENAI_API_KEY=your-key
 *
 * # Run the example
 * cd example-sandboxing
 * ../gradlew bootRun --console=plain
 * </pre>
 *
 * <h2>What to Observe</h2>
 *
 * <p>When the application starts, you'll see warning messages about unsafe tools being wrapped in
 * local activities. Try these prompts:
 *
 * <ul>
 *   <li>"What time is it?" - Calls currentTime() wrapped in local activity
 *   <li>"Generate a random number between 1 and 100" - Calls randomNumber() wrapped
 *   <li>"What is my java.version system property?" - Calls getSystemProperty() wrapped
 * </ul>
 *
 * <p>In the Temporal UI, you'll see local activity markers for each tool call, demonstrating that
 * the tools are being sandboxed for workflow safety.
 */
@SpringBootApplication
public class SandboxingApplication {

  public static void main(String[] args) {
    SpringApplication.run(SandboxingApplication.class, args);
  }
}

@Component
class SandboxingRunner {

  private final WorkflowClient workflowClient;

  SandboxingRunner(WorkflowClient workflowClient) {
    this.workflowClient = workflowClient;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void run() {
    String workflowId = "sandbox-" + UUID.randomUUID().toString().substring(0, 8);

    System.out.println("\n===========================================");
    System.out.println("  Sandboxing Demo - Unsafe Tools Example");
    System.out.println("===========================================");
    System.out.println("Workflow ID: " + workflowId);
    System.out.println("\nThis demo shows how sandboxing mode handles");
    System.out.println("tools that aren't properly annotated.");
    System.out.println("\nWatch for WARN messages about tools being");
    System.out.println("wrapped in local activities.\n");
    System.out.println("Try: 'What time is it?'");
    System.out.println("Try: 'Generate a random number 1-100'");
    System.out.println("Type 'quit' to exit.\n");

    // Start the chat workflow
    SandboxingWorkflow workflow =
        workflowClient.newWorkflowStub(
            SandboxingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue("spring-ai-sandboxing-example")
                .build());

    WorkflowClient.start(
        workflow::run,
        "You are a helpful assistant with access to system tools. "
            + "Use the available tools when asked about time, random numbers, "
            + "or system properties. Be concise.");

    // Get stub for the running workflow
    SandboxingWorkflow chat = workflowClient.newWorkflowStub(SandboxingWorkflow.class, workflowId);

    // Interactive loop
    try (Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8)) {
      while (true) {
        System.out.print("You: ");
        String input = scanner.nextLine().trim();

        if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
          chat.end();
          break;
        }

        if (input.isEmpty()) {
          continue;
        }

        try {
          String response = chat.chat(input);
          System.out.println("Assistant: " + response + "\n");
        } catch (Exception e) {
          System.err.println("Error: " + e.getMessage() + "\n");
        }
      }
    }

    System.out.println("Goodbye!");
    System.exit(0);
  }
}
