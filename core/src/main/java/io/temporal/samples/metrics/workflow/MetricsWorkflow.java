package io.temporal.samples.metrics.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MetricsWorkflow {
  @WorkflowMethod
  String exec(String input);
}
