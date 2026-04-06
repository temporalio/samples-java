package io.temporal.samples.springai.rag;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Workflow interface demonstrating RAG (Retrieval-Augmented Generation).
 *
 * <p>This workflow shows how to use VectorStoreActivity and EmbeddingModelActivity to build a
 * durable knowledge base that can be queried with natural language.
 */
@WorkflowInterface
public interface RagWorkflow {

  /**
   * Runs the workflow until ended.
   *
   * @return summary of the session
   */
  @WorkflowMethod
  String run();

  /**
   * Adds a document to the knowledge base.
   *
   * @param id unique identifier for the document
   * @param content the document content
   */
  @SignalMethod
  void addDocument(String id, String content);

  /**
   * Asks a question using RAG - retrieves relevant documents and generates an answer.
   *
   * @param question the question to answer
   */
  @SignalMethod
  void ask(String question);

  /**
   * Searches for similar documents without generating an answer.
   *
   * @param query the search query
   * @param topK number of results to return
   */
  @SignalMethod
  void search(String query, int topK);

  /**
   * Gets the last response from the AI or search.
   *
   * @return the last response
   */
  @QueryMethod
  String getLastResponse();

  /**
   * Gets the current document count.
   *
   * @return number of documents in the knowledge base
   */
  @QueryMethod
  int getDocumentCount();

  /** Ends the session. */
  @SignalMethod
  void end();
}
