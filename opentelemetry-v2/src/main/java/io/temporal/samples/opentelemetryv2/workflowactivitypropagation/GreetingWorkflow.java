package io.temporal.samples.opentelemetryv2.workflowactivitypropagation;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface GreetingWorkflow {
  String TASK_QUEUE = "workflow-activity-propagation";

  @WorkflowMethod
  String greet(String name);
}
