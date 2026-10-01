package io.temporal.samples.nexusserializationcontext.caller;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CallerWorkflow {
  @WorkflowMethod
  EndpointResults echoThroughEndpoints(String message);
}
