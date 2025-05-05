package io.temporal.samples.polling.infrequentwithretryafter;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.samples.polling.PollingActivities;
import io.temporal.samples.polling.PollingWorkflow;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class InfrequentPollingWithRetryAfterWorkflowImpl implements PollingWorkflow {
  @Override
  public String exec() {
    /*
     * Infrequent polling via activity can be implemented via activity retries. For this sample we
     * want to poll the test service initially 60 seconds. After that we want to retry it based on
     * the Retry-After directive from the downstream servie we are invoking from the activity.
     *
     * <ol>
     *   <li>Set RetryPolicy backoff coefficient of 1
     *   <li>Set initial interval to the poll frequency (since coefficient is 1, same interval will
     *       be used as default retry attempt)
     * </ol>
     */
    ActivityOptions options =
        ActivityOptions.newBuilder()
            // Set activity StartToClose timeout (single activity exec), does not include retries
            .setStartToCloseTimeout(Duration.ofSeconds(2))
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setBackoffCoefficient(1)
                    // note we don't set initial interval here
                    .build())
            .build();
    // create our activities stub and start activity execution
    PollingActivities activities = Workflow.newActivityStub(PollingActivities.class, options);
    return activities.doPoll();
  }
}
