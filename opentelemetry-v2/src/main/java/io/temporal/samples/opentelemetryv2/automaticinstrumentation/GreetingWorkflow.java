package io.temporal.samples.opentelemetryv2.automaticinstrumentation;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface GreetingWorkflow {
  String TASK_QUEUE = "automatic-instrumentation";

  @WorkflowMethod
  String greet(String name);
}
