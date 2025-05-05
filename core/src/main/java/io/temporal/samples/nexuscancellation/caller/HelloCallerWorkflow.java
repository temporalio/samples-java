

package io.temporal.samples.nexuscancellation.caller;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloCallerWorkflow {
  @WorkflowMethod
  String hello(String message);
}
