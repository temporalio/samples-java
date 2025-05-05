package io.temporal.samples.springboot.kafka;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MessageWorkflow {

  @WorkflowMethod
  void start();

  @SignalMethod
  void update(String message);
}
