

package io.temporal.samples.polling.frequent;

import io.temporal.activity.ActivityOptions;
import io.temporal.samples.polling.PollingActivities;
import io.temporal.samples.polling.PollingWorkflow;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class FrequentPollingWorkflowImpl implements PollingWorkflow {
  @Override
  public String exec() {
    /*
     * Frequent polling (1 second or faster) should be done inside the activity itself. Note that
     * the activity has to heart beat on each iteration. Note that we need to set our
     * HeartbeatTimeout in ActivityOptions shorter than the StartToClose timeout. You can use an
     * appropriate activity retry policy for your activity.
     */
    ActivityOptions options =
        ActivityOptions.newBuilder()
            // Set activity StartToClose timeout (single activity exec), does not include retries
            .setStartToCloseTimeout(Duration.ofSeconds(60))
            .setHeartbeatTimeout(Duration.ofSeconds(2))
            // For sample we just use the default retry policy (do not set explicitly)
            .build();
    // create our activities stub and start activity execution
    PollingActivities activities = Workflow.newActivityStub(PollingActivities.class, options);
    return activities.doPoll();
  }
}
