package io.temporal.samples.polling.infrequentwithretryafter;

import io.temporal.activity.Activity;
import io.temporal.failure.ApplicationErrorCategory;
import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.polling.PollingActivities;
import io.temporal.samples.polling.TestService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class InfrequentPollingWithRetryAfterActivityImpl implements PollingActivities {
  private final TestService service;
  final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

  public InfrequentPollingWithRetryAfterActivityImpl(TestService service) {
    this.service = service;
  }

  @Override
  public String doPoll() {
    System.out.println(
        "Attempt: "
            + Activity.getExecutionContext().getInfo().getAttempt()
            + " Poll time: "
            + LocalDateTime.now(ZoneId.systemDefault()).format(ISO_FORMATTER));

    try {
      return service.getServiceResult();
    } catch (TestService.TestServiceException e) {
      // we throw application failure that includes cause
      // which is the test service exception
      // and delay which is the interval to next retry based on test service retry-after directive
      System.out.println("Activity next retry in: " + e.getRetryAfterInMinutes() + " minutes");
      throw ApplicationFailure.newBuilder()
          .setMessage(e.getMessage())
          .setType(e.getClass().getName())
          .setCause(e)
          // Here we set the next retry interval based on Retry-After duration given to us by our
          // service
          .setNextRetryDelay(Duration.ofMinutes(e.getRetryAfterInMinutes()))
          // This failure is expected so we set it as benign to avoid excessive logging
          .setCategory(ApplicationErrorCategory.BENIGN)
          .build();
    }
  }
}
