package io.temporal.samples.polling.periodicsequence;

import io.temporal.samples.polling.PollingWorkflow;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;

public class PeriodicPollingWorkflowImpl implements PollingWorkflow {

  // Set some periodic poll interval, for sample we set 5 seconds
  private int pollingIntervalInSeconds = 5;

  @Override
  public String exec() {
    PollingChildWorkflow childWorkflow =
        Workflow.newChildWorkflowStub(
            PollingChildWorkflow.class,
            ChildWorkflowOptions.newBuilder().setWorkflowId("ChildWorkflowPoll").build());

    return childWorkflow.exec(pollingIntervalInSeconds);
  }
}
