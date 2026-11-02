package io.temporal.samples.opentelemetryv2.metrics;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface GreetingWorkflow {
  @WorkflowMethod
  String greet(String name);
}
