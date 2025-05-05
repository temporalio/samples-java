

package io.temporal.samples.tracing.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface TracingChildWorkflow {
  @WorkflowMethod
  String greet(String name, String language);
}
