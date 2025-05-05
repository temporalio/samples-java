

package io.temporal.samples.tracing.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class TracingChildWorkflowImpl implements TracingChildWorkflow {
  @Override
  public String greet(String name, String language) {

    ActivityOptions activityOptions =
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build();
    TracingActivities activities =
        Workflow.newActivityStub(TracingActivities.class, activityOptions);

    return activities.greet(name, language);
  }
}
