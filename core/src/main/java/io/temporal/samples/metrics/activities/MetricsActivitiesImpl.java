package io.temporal.samples.metrics.activities;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;

public class MetricsActivitiesImpl implements MetricsActivities {

  @Override
  public String performA(String input) {
    // simulate some failures to trigger retries
    if (Activity.getExecutionContext().getInfo().getAttempt() < 3) {
      incRetriesCustomMetric(Activity.getExecutionContext());
      throw Activity.wrap(new NullPointerException("simulated"));
    }
    return "Performed activity A with input " + input + "\n";
  }

  @Override
  public String performB(String input) {
    // simulate some failures to trigger retries
    if (Activity.getExecutionContext().getInfo().getAttempt() < 5) {
      incRetriesCustomMetric(Activity.getExecutionContext());
      throw Activity.wrap(new NullPointerException("simulated"));
    }
    return "Performed activity B with input " + input + "\n";
  }

  private void incRetriesCustomMetric(ActivityExecutionContext context) {
    // We can create a child scope and add extra tags
    //    Scope scope =
    //        context
    //            .getMetricsScope()
    //            .tagged(
    //                Stream.of(
    //                        new String[][] {
    //                          {"workflow_id", context.getInfo().getWorkflowId()},
    //                          {"activity_id", context.getInfo().getActivityId()},
    //                          {
    //                            "activity_start_to_close_timeout",
    //                            context.getInfo().getStartToCloseTimeout().toString()
    //                          },
    //                        })
    //                    .collect(Collectors.toMap(data -> data[0], data -> data[1])));
    //
    //    scope.counter("custom_activity_retries").inc(1);

    // For sample we use root scope
    context.getMetricsScope().counter("custom_activity_retries").inc(1);
  }
}
