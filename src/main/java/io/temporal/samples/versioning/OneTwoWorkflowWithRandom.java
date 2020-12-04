package io.temporal.samples.versioning;

import static io.temporal.samples.versioning.OneTwoWorker.TASK_QUEUE;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Random;

public class OneTwoWorkflowWithRandom implements OneTwoWorkflow {

  private boolean signaled;

  @Override
  public int run() {
    ActivityOptions options =
        ActivityOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setScheduleToCloseTimeout(Duration.ofSeconds(10))
            .build();
    OneTwoActivities activities = Workflow.newActivityStub(OneTwoActivities.class, options);
    boolean useOne = Workflow.sideEffect(Boolean.class, () -> new Random().nextBoolean());
    int number;
    if (useOne) {
      number = activities.one();
    } else {
      number = activities.two();
    }
    Workflow.await(() -> signaled);

    return number;
  }

  @Override
  public void signal() {
    signaled = true;
  }
}
