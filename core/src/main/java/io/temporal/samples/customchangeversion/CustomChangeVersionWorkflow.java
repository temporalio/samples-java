

package io.temporal.samples.customchangeversion;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CustomChangeVersionWorkflow {
  @WorkflowMethod
  String run(String input);
}
