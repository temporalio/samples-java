

package io.temporal.samples.ssl;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MyWorkflow {
  @WorkflowMethod
  String execute();
}
