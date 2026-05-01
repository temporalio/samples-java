package io.temporal.samples.springai.rag;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.springai.activity.VectorStoreActivity;
import io.temporal.springai.chat.TemporalChatClient;
import io.temporal.springai.model.ActivityChatModel;
import io.temporal.springai.model.VectorStoreTypes;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Implementation of the RAG workflow.
 *
 * <p>This demonstrates:
 *
 * <ul>
 *   <li>Using {@link VectorStoreActivity} to store and search documents (the configured {@code
 *       VectorStore} handles embedding internally)
 *   <li>Combining vector search with chat for RAG
 * </ul>
 *
 * <p>All operations are durable Temporal activities - if the worker restarts, the workflow will
 * continue from where it left off.
 */
public class RagWorkflowImpl implements RagWorkflow {

  private final VectorStoreActivity vectorStore;
  private final ChatClient chatClient;

  private String lastResponse = "";
  private int documentCount = 0;
  private boolean ended = false;

  @WorkflowInit
  public RagWorkflowImpl() {
    // Create activity stubs with appropriate timeouts
    ActivityOptions activityOptions =
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(2))
            .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
            .build();

    this.vectorStore = Workflow.newActivityStub(VectorStoreActivity.class, activityOptions);

    // Create the chat client
    ActivityChatModel chatModel = ActivityChatModel.forDefault();
    this.chatClient =
        TemporalChatClient.builder(chatModel)
            .defaultSystem(
                """
                You are a helpful assistant that answers questions based on the provided context.

                When answering:
                - Use only the information from the context provided
                - If the context doesn't contain relevant information, say so
                - Be concise and direct
                """)
            .build();
  }

  @Override
  public String run() {
    Workflow.await(() -> ended);
    return "Session ended. Processed " + documentCount + " documents.";
  }

  @Override
  public void addDocument(String id, String content) {
    // Create a document and add it to the vector store
    // The vector store will use the embedding model to generate embeddings
    VectorStoreTypes.Document doc = new VectorStoreTypes.Document(id, content);
    vectorStore.addDocuments(new VectorStoreTypes.AddDocumentsInput(List.of(doc)));

    documentCount++;
    lastResponse =
        "Added document '" + id + "' to knowledge base. Total documents: " + documentCount;
  }

  @Override
  public void ask(String question) {
    // Step 1: Search for relevant documents
    VectorStoreTypes.SearchOutput searchResults =
        vectorStore.similaritySearch(new VectorStoreTypes.SearchInput(question, 3));

    if (searchResults.documents().isEmpty()) {
      lastResponse = "No relevant documents found in the knowledge base.";
      return;
    }

    // Step 2: Build context from search results
    String context =
        searchResults.documents().stream()
            .map(result -> result.document().text())
            .collect(Collectors.joining("\n\n---\n\n"));

    // Step 3: Generate answer using the context
    lastResponse =
        chatClient
            .prompt()
            .user(
                u ->
                    u.text(
                            """
                            Context:
                            {context}

                            Question: {question}

                            Answer based on the context above:
                            """)
                        .param("context", context)
                        .param("question", question))
            .call()
            .content();
  }

  @Override
  public void search(String query, int topK) {
    VectorStoreTypes.SearchOutput searchResults =
        vectorStore.similaritySearch(new VectorStoreTypes.SearchInput(query, topK));

    if (searchResults.documents().isEmpty()) {
      lastResponse = "No matching documents found.";
      return;
    }

    StringBuilder sb = new StringBuilder("Search results:\n\n");
    for (int i = 0; i < searchResults.documents().size(); i++) {
      VectorStoreTypes.SearchResult result = searchResults.documents().get(i);
      sb.append(
          String.format(
              "%d. [Score: %.3f] %s\n   %s\n\n",
              i + 1,
              result.score(),
              result.document().id(),
              truncate(result.document().text(), 100)));
    }
    lastResponse = sb.toString();
  }

  @Override
  public String getLastResponse() {
    return lastResponse;
  }

  @Override
  public int getDocumentCount() {
    return documentCount;
  }

  @Override
  public void end() {
    ended = true;
  }

  private String truncate(String text, int maxLength) {
    if (text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength) + "...";
  }
}
