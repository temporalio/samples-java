package io.temporal.samples.workflowstreams;

import io.temporal.samples.workflowstreams.Shared.OrderInput;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Scenarios 1 and 2: publishes status events directly from workflow code and runs an activity that
 * publishes fine-grained progress events to the same stream. A subscriber consumes both topics.
 */
@WorkflowInterface
public interface OrderWorkflow {
  @WorkflowMethod
  String processOrder(OrderInput input);
}
