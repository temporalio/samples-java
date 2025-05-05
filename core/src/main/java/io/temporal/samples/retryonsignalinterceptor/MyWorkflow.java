

package io.temporal.samples.retryonsignalinterceptor;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MyWorkflow {

  @WorkflowMethod
  void execute();
}
