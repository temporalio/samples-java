package io.temporal.samples.workflowstreams;

import io.temporal.samples.workflowstreams.Shared.HubInput;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflowstreams.WorkflowStream;

public class HubWorkflowImpl implements HubWorkflow {

  private boolean closed;

  @WorkflowInit
  public HubWorkflowImpl(HubInput input) {
    WorkflowStream.newInstance(input.streamState);
  }

  @Override
  public String host(HubInput input) {
    Workflow.await(() -> closed);

    // The publisher publishes its own terminator into the stream before signaling close
    // (see ExternalPublisher). Hold the run open briefly so subscribers' final poll
    // delivers any items still in the log.
    Workflow.sleep(OrderWorkflowImpl.DRAIN_DELAY);
    return "hub " + input.hubId + " closed";
  }

  @Override
  public void close() {
    closed = true;
  }
}
