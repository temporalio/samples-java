package io.temporal.samples.workflowtimeout;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class MyWorkflowImpl implements MyWorkflow {

  private final MyActivity activity =
      Workflow.newActivityStub(
          MyActivity.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(2))
              .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
              .build());

  //  private boolean done;

  @Override
  public int run() {
    Workflow.sleep(Duration.ofSeconds(3));
    // Workflow.await(() -> done);
    return 8;
  }

  @Override
  public int myUpdate() {
    Workflow.sleep(Duration.ofSeconds(2));
    int result = activity.myActivityMethod();
    //  done = true;
    return result;
  }
}
