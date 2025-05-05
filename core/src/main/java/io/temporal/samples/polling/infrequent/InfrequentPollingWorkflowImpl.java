package io.temporal.samples.polling.infrequent;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.samples.polling.PollingActivities;
import io.temporal.samples.polling.PollingWorkflow;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class InfrequentPollingWorkflowImpl implements PollingWorkflow {
  @Override
  public String exec() {
    /*
     * Infrequent polling via activity can be implemented via activity retries. For this sample we
     * want to poll the test service every 60 seconds. Here we:
     *
     * <ol>
     *   <li>Set RetryPolicy backoff coefficient of 1
     *   <li>Set initial interval to the poll frequency (since coefficient is 1, same interval will
     *       be used for all retries)
     * </ol>
     *
     * <p>With this in case our test service is "down" we can fail our activity and it will be
     * retried based on our 60 second retry interval until poll is successful and we can return a
     * result from the activity.
     */
    ActivityOptions options =
        ActivityOptions.newBuilder()
            // Set activity StartToClose timeout (single activity exec), does not include retries
            .setStartToCloseTimeout(Duration.ofSeconds(2))
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setBackoffCoefficient(1)
                    .setInitialInterval(Duration.ofSeconds(60))
                    .build())
            .build();
    // create our activities stub and start activity execution
    PollingActivities activities = Workflow.newActivityStub(PollingActivities.class, options);
    return activities.doPoll();
  }
}
