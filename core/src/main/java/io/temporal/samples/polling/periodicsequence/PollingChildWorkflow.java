package io.temporal.samples.polling.periodicsequence;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PollingChildWorkflow {
  @WorkflowMethod
  String exec(int pollingIntervalInSeconds);
}
