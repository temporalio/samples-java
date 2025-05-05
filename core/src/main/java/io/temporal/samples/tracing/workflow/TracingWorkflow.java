

package io.temporal.samples.tracing.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface TracingWorkflow {

  @WorkflowMethod
  String greet(String name);

  @SignalMethod
  void setLanguage(String language);

  @QueryMethod
  String getLanguage();
}
