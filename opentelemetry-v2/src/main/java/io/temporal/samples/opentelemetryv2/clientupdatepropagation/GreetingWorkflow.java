package io.temporal.samples.opentelemetryv2.clientupdatepropagation;

import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.UpdateValidatorMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface GreetingWorkflow {
  String TASK_QUEUE = "client-update-propagation";

  @WorkflowMethod
  String run();

  @UpdateMethod
  String greet(String name);

  @UpdateValidatorMethod(updateName = "greet")
  void validateGreet(String name);
}
