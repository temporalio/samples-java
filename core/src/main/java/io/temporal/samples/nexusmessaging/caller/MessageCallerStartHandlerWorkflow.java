package io.temporal.samples.nexusmessaging.caller;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MessageCallerStartHandlerWorkflow {
  @WorkflowMethod
  String sentMessage();
}
