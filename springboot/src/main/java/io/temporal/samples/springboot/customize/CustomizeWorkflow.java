package io.temporal.samples.springboot.customize;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CustomizeWorkflow {
  @WorkflowMethod
  String execute();
}
