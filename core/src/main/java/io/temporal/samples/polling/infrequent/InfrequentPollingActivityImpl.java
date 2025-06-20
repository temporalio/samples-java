package io.temporal.samples.polling.infrequent;

import io.temporal.failure.ApplicationErrorCategory;
import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.polling.PollingActivities;
import io.temporal.samples.polling.TestService;

public class InfrequentPollingActivityImpl implements PollingActivities {
  private final TestService service;

  public InfrequentPollingActivityImpl(TestService service) {
    this.service = service;
  }

  @Override
  public String doPoll() {
    try {
      return service.getServiceResult();
    } catch (TestService.TestServiceException e) {
      // We want to rethrow the service exception so we can poll via activity retries
      throw ApplicationFailure.newBuilder()
          .setMessage(e.getMessage())
          .setType(e.getClass().getName())
          .setCause(e)
          // This failure is expected so we set it as benign to avoid excessive logging
          .setCategory(ApplicationErrorCategory.BENIGN)
          .build();
    }
  }
}
