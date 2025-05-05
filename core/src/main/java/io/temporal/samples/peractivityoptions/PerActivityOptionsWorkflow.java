package io.temporal.samples.peractivityoptions;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PerActivityOptionsWorkflow {
  @WorkflowMethod
  void execute();
}
