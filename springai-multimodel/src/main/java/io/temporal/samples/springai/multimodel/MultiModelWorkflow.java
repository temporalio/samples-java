package io.temporal.samples.springai.multimodel;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Workflow interface demonstrating multiple chat models.
 *
 * <p>This workflow shows how to use different AI models for different purposes within the same
 * workflow.
 */
@WorkflowInterface
public interface MultiModelWorkflow {

  /**
   * Runs the workflow until ended.
   *
   * @return summary of the chat session
   */
  @WorkflowMethod
  String run();

  /**
   * Sends a message to a specific model.
   *
   * @param modelName the name of the model to use ("fast", "smart", or "default")
   * @param message the user message
   */
  @SignalMethod
  void chat(String modelName, String message);

  /**
   * Gets the last response.
   *
   * @return the last response from any model
   */
  @QueryMethod
  String getLastResponse();

  /** Ends the chat session. */
  @SignalMethod
  void end();
}
