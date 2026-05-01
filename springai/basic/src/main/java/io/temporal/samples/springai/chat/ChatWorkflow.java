package io.temporal.samples.springai.chat;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * A chat workflow that maintains a conversation with an AI model.
 *
 * <p>The workflow runs until explicitly ended via the {@link #end()} signal. Messages can be sent
 * via the {@link #chat(String)} update method, which returns the AI's response synchronously.
 */
@WorkflowInterface
public interface ChatWorkflow {

  /**
   * Starts the chat workflow and waits until ended.
   *
   * @param systemPrompt the system prompt that defines the AI's behavior
   * @return a summary when the chat ends
   */
  @WorkflowMethod
  String run(String systemPrompt);

  /**
   * Sends a message to the AI and returns its response.
   *
   * @param message the user's message
   * @return the AI's response
   */
  @UpdateMethod
  String chat(String message);

  /** Ends the chat session. */
  @SignalMethod
  void end();
}
