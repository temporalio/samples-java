package io.temporal.samples.workflowstreams;

import io.temporal.samples.workflowstreams.Shared.HubInput;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Scenario 4: does no work of its own; it exists only to host the stream for an external publisher
 * and shuts down on a close signal.
 */
@WorkflowInterface
public interface HubWorkflow {
  @WorkflowMethod
  String host(HubInput input);

  @SignalMethod
  void close();
}
