

package io.temporal.samples.asyncuntypedchild;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Define the child workflow Interface. It must contain one method annotated with @WorkflowMethod
 *
 * @see WorkflowInterface
 * @see WorkflowMethod
 */
@WorkflowInterface
public interface ChildWorkflow {

  /**
   * Define the child workflow method. This method is executed when the workflow is started. The
   * workflow completes when the workflow method finishes execution.
   */
  @WorkflowMethod
  String composeGreeting(String greeting, String name);
}
