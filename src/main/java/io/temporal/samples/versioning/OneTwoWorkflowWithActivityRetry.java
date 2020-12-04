package io.temporal.samples.versioning;

import static io.temporal.samples.versioning.OneTwoWorker.TASK_QUEUE;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class OneTwoWorkflowWithActivityRetry implements OneTwoWorkflow {

  private boolean signaled;

  @Override
  public int run() {
    ActivityOptions options =
        ActivityOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setScheduleToCloseTimeout(Duration.ofSeconds(60))
            .setRetryOptions(RetryOptions.newBuilder().build())
            .build();
    OneTwoActivities activities = Workflow.newActivityStub(OneTwoActivities.class, options);
    return activities.oneWithRetries(6);
  }

  @Override
  public void signal() {
    signaled = true;
  }
}
