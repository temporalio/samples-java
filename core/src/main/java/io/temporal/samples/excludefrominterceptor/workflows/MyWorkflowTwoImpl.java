package io.temporal.samples.excludefrominterceptor.workflows;

import io.temporal.activity.ActivityOptions;
import io.temporal.samples.excludefrominterceptor.activities.MyActivities;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class MyWorkflowTwoImpl implements MyWorkflowTwo {
  private MyActivities activities =
      Workflow.newActivityStub(
          MyActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

  @Override
  public String execute(String input) {
    activities.activityOne(input);
    activities.activityTwo(input);

    return "done";
  }
}
