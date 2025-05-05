package io.temporal.samples.listworkflows;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CustomerWorkflow {
  @WorkflowMethod
  void updateAccountMessage(Customer customer, String message);

  @SignalMethod
  void exit();
}
