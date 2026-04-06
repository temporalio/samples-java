package io.temporal.samples.springai.chat;

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
 * Example application demonstrating the Spring AI Temporal plugin.
 *
 * <p>Starts an interactive chat workflow where each AI call is a durable Temporal activity with
 * automatic retries and timeout handling.
 */
@SpringBootApplication
public class ChatExampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(ChatExampleApplication.class, args);
  }
}

@Component
class ChatRunner {

  private final WorkflowClient workflowClient;

  ChatRunner(WorkflowClient workflowClient) {
    this.workflowClient = workflowClient;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void run() {
    String workflowId = "chat-" + UUID.randomUUID().toString().substring(0, 8);

    System.out.println("\n===========================================");
    System.out.println("  Spring AI + Temporal Chat Demo");
    System.out.println("===========================================");
    System.out.println("Workflow ID: " + workflowId);
    System.out.println("Type messages, or 'quit' to exit.\n");

    // Start the chat workflow
    ChatWorkflow workflow =
        workflowClient.newWorkflowStub(
            ChatWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue("spring-ai-example")
                .build());

    WorkflowClient.start(workflow::run, "You are a helpful assistant. Be concise.");

    // Get stub for the running workflow
    ChatWorkflow chat = workflowClient.newWorkflowStub(ChatWorkflow.class, workflowId);

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
