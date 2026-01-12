package io.temporal.samples.temporalcloudopenmetrics.workflows;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ScenarioWorkflow {
  @WorkflowMethod
  String run(String scenario, String name);
}
