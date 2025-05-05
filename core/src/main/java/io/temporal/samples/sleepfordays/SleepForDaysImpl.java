

package io.temporal.samples.sleepfordays;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class SleepForDaysImpl implements SleepForDaysWorkflow {

  private final SendEmailActivity activity;
  private boolean complete = false;

  public SleepForDaysImpl() {
    this.activity =
        Workflow.newActivityStub(
            SendEmailActivity.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());
  }

  @Override
  public String sleepForDays() {
    while (!this.complete) {
      activity.sendEmail(String.format("Sleeping for 30 days"));
      Promise<Void> timer = Workflow.newTimer(Duration.ofDays(30));
      Workflow.await(() -> timer.isCompleted() || this.complete);
    }

    return "done!";
  }

  @Override
  public void complete() {
    this.complete = true;
  }
}
