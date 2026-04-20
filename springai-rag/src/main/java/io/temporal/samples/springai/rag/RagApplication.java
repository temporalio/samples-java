package io.temporal.samples.springai.rag;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import java.util.Scanner;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example application demonstrating RAG with VectorStore and Embeddings.
 *
 * <p>This application shows how to use the plugin's VectorStoreActivity and EmbeddingModelActivity
 * to build a durable knowledge base within Temporal workflows.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * Commands:
 *   add &lt;id&gt; &lt;content&gt;  - Add a document to the knowledge base
 *   ask &lt;question&gt;      - Ask a question (uses RAG)
 *   search &lt;query&gt;      - Search for similar documents
 *   count               - Show document count
 *   quit                - End the session
 * </pre>
 *
 * <h2>Prerequisites</h2>
 *
 * <ol>
 *   <li>Start a Temporal dev server: {@code temporal server start-dev}
 *   <li>Set OPENAI_API_KEY environment variable
 *   <li>Run: {@code ./gradlew :example-rag:bootRun}
 * </ol>
 */
@SpringBootApplication
public class RagApplication implements CommandLineRunner {

  private static final String TASK_QUEUE = "rag-example-queue";

  @Autowired private WorkflowClient workflowClient;

  public static void main(String[] args) {
    SpringApplication.run(RagApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    // Start a new workflow
    String workflowId = "rag-example-" + UUID.randomUUID().toString().substring(0, 8);
    RagWorkflow workflow =
        workflowClient.newWorkflowStub(
            RagWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId(workflowId)
                .build());

    // Start the workflow asynchronously
    WorkflowClient.start(workflow::run);

    System.out.println("\n=== RAG (Retrieval-Augmented Generation) Demo ===");
    System.out.println("Workflow ID: " + workflowId);
    System.out.println("\nThis demo uses VectorStoreActivity and EmbeddingModelActivity");
    System.out.println("to build a durable knowledge base with semantic search.");
    System.out.println("\nCommands:");
    System.out.println("  add <id> <content>  - Add a document");
    System.out.println("  ask <question>      - Ask a question (RAG)");
    System.out.println("  search <query>      - Search documents");
    System.out.println("  count               - Show document count");
    System.out.println("  quit                - End session");
    System.out.println("\nTry adding some documents first, then ask questions about them!");
    System.out.println();

    // Get a workflow stub for sending signals/queries
    RagWorkflow workflowStub = workflowClient.newWorkflowStub(RagWorkflow.class, workflowId);

    Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);
    while (true) {
      System.out.print("> ");
      String input = scanner.nextLine().trim();

      if (input.equalsIgnoreCase("quit")) {
        workflowStub.end();
        System.out.println("Session ended. Goodbye!");
        break;
      }

      if (input.equalsIgnoreCase("count")) {
        System.out.println("Documents in knowledge base: " + workflowStub.getDocumentCount());
        continue;
      }

      if (input.startsWith("add ")) {
        String rest = input.substring(4).trim();
        int spaceIndex = rest.indexOf(' ');
        if (spaceIndex == -1) {
          System.out.println("Usage: add <id> <content>");
          continue;
        }
        String id = rest.substring(0, spaceIndex);
        String content = rest.substring(spaceIndex + 1).trim();

        System.out.println("[Adding document...]");
        workflowStub.addDocument(id, content);
        waitForResponse(workflowStub);
        continue;
      }

      if (input.equals("ask") || input.startsWith("ask ")) {
        String question = input.length() > 3 ? input.substring(4).trim() : "";
        if (question.isEmpty()) {
          System.out.println("Usage: ask <question>");
          continue;
        }

        System.out.println("[Searching and generating answer...]");
        workflowStub.ask(question);
        waitForResponse(workflowStub);
        continue;
      }

      if (input.startsWith("search ")) {
        String query = input.substring(7).trim();
        if (query.isEmpty()) {
          System.out.println("Usage: search <query>");
          continue;
        }

        System.out.println("[Searching...]");
        workflowStub.search(query, 5);
        waitForResponse(workflowStub);
        continue;
      }

      if (!input.isEmpty()) {
        System.out.println("Unknown command. Use: add, ask, search, count, or quit");
      }
    }
  }

  private void waitForResponse(RagWorkflow workflowStub) throws InterruptedException {
    String lastResponse = workflowStub.getLastResponse();
    for (int i = 0; i < 600; i++) { // Wait up to 60 seconds
      Thread.sleep(100);
      String response = workflowStub.getLastResponse();
      if (!response.equals(lastResponse)) {
        System.out.println("\n" + response + "\n");
        return;
      }
    }
    System.out.println("[Timeout waiting for response]");
  }
}
