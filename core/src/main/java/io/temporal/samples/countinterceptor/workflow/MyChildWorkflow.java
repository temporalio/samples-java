

package io.temporal.samples.countinterceptor.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MyChildWorkflow {
  @WorkflowMethod
  String execChild(String name, String title);
}
