package io.temporal.samples.lambdaworker;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/** Sample workflow run by the Lambda worker. */
@WorkflowInterface
public interface SampleWorkflow {

  @WorkflowMethod
  String getGreeting(String name);
}
