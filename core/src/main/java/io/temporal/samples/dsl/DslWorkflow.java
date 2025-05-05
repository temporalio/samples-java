package io.temporal.samples.dsl;

import io.temporal.samples.dsl.model.Flow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DslWorkflow {
  @WorkflowMethod
  String run(Flow flow, String input);
}
