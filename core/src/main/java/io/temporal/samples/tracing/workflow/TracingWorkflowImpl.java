package io.temporal.samples.tracing.workflow;

import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;

public class TracingWorkflowImpl implements TracingWorkflow {

  private String language = "English";

  @Override
  public String greet(String name) {
    ChildWorkflowOptions options =
        ChildWorkflowOptions.newBuilder().setWorkflowId("tracingChildWorkflow").build();

    // Get the child workflow stub
    TracingChildWorkflow child = Workflow.newChildWorkflowStub(TracingChildWorkflow.class, options);

    // Invoke child sync and return its result
    return child.greet(name, language);
  }

  @Override
  public void setLanguage(String language) {
    this.language = language;
  }

  @Override
  public String getLanguage() {
    return language;
  }
}
