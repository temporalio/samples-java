package io.temporal.samples.complex;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ComplexWorkflow {
  String TASK_QUEUE = "complex";

  @WorkflowMethod
  void handleLambda(Input input);
}
