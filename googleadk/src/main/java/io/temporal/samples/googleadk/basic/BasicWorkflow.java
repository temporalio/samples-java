package io.temporal.samples.googleadk.basic;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface BasicWorkflow {
  @WorkflowMethod
  String ask(String question);
}
