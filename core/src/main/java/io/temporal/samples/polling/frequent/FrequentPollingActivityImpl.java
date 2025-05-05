package io.temporal.samples.polling.frequent;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.client.ActivityCompletionException;
import io.temporal.samples.polling.PollingActivities;
import io.temporal.samples.polling.TestService;
import java.util.concurrent.TimeUnit;

public class FrequentPollingActivityImpl implements PollingActivities {
  private final TestService service;
  private static final int POLL_DURATION_SECONDS = 1;

  public FrequentPollingActivityImpl(TestService service) {
    this.service = service;
  }

  @Override
  public String doPoll() {
    ActivityExecutionContext context = Activity.getExecutionContext();

    // Here we implement our polling inside the activity impl
    while (true) {
      try {
        return service.getServiceResult();
      } catch (TestService.TestServiceException e) {
        // service "down" we can log
      }

      // heart beat and sleep for the poll duration
      try {
        context.heartbeat(null);
      } catch (ActivityCompletionException e) {
        // activity was either cancelled or workflow was completed or worker shut down
        throw e;
      }
      sleep(POLL_DURATION_SECONDS);
    }
  }

  private void sleep(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
