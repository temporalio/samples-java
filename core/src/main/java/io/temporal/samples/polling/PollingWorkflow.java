package io.temporal.samples.polling;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PollingWorkflow {
  @WorkflowMethod
  String exec();
}
