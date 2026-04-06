package io.temporal.samples.springai.sandboxing;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Workflow interface for the sandboxing demonstration.
 *
 * <p>This is a simple chat workflow that demonstrates how unsafe tools can be sandboxed to prevent
 * non-deterministic behavior.
 */
@WorkflowInterface
public interface SandboxingWorkflow {

  /**
   * Main workflow method. Waits for the chat session to end.
   *
   * @param systemPrompt the system prompt to configure the AI assistant
   * @return a summary of the chat session
   */
  @WorkflowMethod
  String run(String systemPrompt);

  /**
   * Sends a message to the AI and receives a response.
   *
   * @param message the user's message
   * @return the AI's response
   */
  @UpdateMethod
  String chat(String message);

  /** Signals the workflow to end the chat session. */
  @SignalMethod
  void end();
}
