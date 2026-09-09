package io.temporal.samples.gcp.cloudrun.workerid;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/** A small greeting workflow run by the Cloud Run worker. */
@WorkflowInterface
public interface GreetingWorkflow {

  @WorkflowMethod
  String getGreeting(String name);
}
