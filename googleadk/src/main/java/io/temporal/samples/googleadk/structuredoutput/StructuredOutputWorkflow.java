package io.temporal.samples.googleadk.structuredoutput;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface StructuredOutputWorkflow {
  @WorkflowMethod
  TravelPlan plan(String request);
}
