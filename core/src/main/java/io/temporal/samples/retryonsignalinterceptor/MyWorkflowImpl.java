package io.temporal.samples.retryonsignalinterceptor;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class MyWorkflowImpl implements MyWorkflow {

  private final MyActivity activity =
      Workflow.newActivityStub(
          MyActivity.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(30))
              // disable server side retries. In most production applications the retries should be
              // done for a while before requiring an external operator signal.
              .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
              .build());

  @Override
  public void execute() {
    activity.execute();
  }
}
