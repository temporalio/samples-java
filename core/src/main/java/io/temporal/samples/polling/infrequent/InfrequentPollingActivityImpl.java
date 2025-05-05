

package io.temporal.samples.polling.infrequent;

import io.temporal.activity.Activity;
import io.temporal.samples.polling.PollingActivities;
import io.temporal.samples.polling.TestService;

public class InfrequentPollingActivityImpl implements PollingActivities {
  private TestService service;

  public InfrequentPollingActivityImpl(TestService service) {
    this.service = service;
  }

  @Override
  public String doPoll() {
    try {
      return service.getServiceResult();
    } catch (TestService.TestServiceException e) {
      // We want to rethrow the service exception so we can poll via activity retries
      throw Activity.wrap(e);
    }
  }
}
