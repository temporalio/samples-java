package io.temporal.samples.asyncuntypedchild;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Define the parent workflow interface. It must contain one method annotated with @WorkflowMethod
 *
 * @see WorkflowInterface
 * @see WorkflowMethod
 */
@WorkflowInterface
public interface ParentWorkflow {

  /**
   * Define the parent workflow method. This method is executed when the workflow is started. The
   * workflow completes when the workflow method finishes execution.
   */
  @WorkflowMethod
  String getGreeting(String name);
}
