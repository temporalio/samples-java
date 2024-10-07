package io.temporal.samples.workflowtimeout;

import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MyWorkflow {
  @WorkflowMethod
  int run();

  @UpdateMethod
  int myUpdate();
}
