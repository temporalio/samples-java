

package io.temporal.samples.excludefrominterceptor.workflows;

import io.temporal.workflow.WorkflowMethod;

public interface MyWorkflow {
  @WorkflowMethod
  String execute(String input);
}
