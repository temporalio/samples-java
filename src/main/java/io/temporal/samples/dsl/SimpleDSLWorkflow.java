package io.temporal.samples.dsl;

import io.temporal.samples.dsl.models.DslWorkflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface SimpleDSLWorkflow {
  @WorkflowMethod
  void execute(DslWorkflow workflow);
}
