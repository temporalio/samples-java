package io.temporal.samples.nexusserializationcontext.handler;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface EchoWorkflow {
  @WorkflowMethod
  String echo(String message);
}
