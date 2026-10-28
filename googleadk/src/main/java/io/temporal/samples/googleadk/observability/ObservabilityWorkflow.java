package io.temporal.samples.googleadk.observability;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ObservabilityWorkflow {
  @WorkflowMethod
  String observe(String prompt);
}
