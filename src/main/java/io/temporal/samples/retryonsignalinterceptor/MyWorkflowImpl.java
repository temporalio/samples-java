package io.temporal.samples.retryonsignalinterceptor;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class MyWorkflowImpl implements MyWorkflow {

  private final MyActivity activity =
      Workflow.newActivityStub(
          MyActivity.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30)).build());

  @Override
  public void execute() {}
}
