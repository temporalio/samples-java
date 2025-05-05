package io.temporal.samples.listworkflows;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerActivitiesImpl implements CustomerActivities {

  private static final Logger log = LoggerFactory.getLogger(CustomerActivitiesImpl.class);

  @Override
  public void getCustomerAccount(Customer customer) {
    // simulate some actual work...
    sleepSeconds(1);
  }

  @Override
  public void updateCustomerAccount(Customer customer, String message) {
    // simulate some actual work...
    sleepSeconds(1);
  }

  @Override
  public void sendUpdateEmail(Customer customer) {
    // simulate some actual work...
    sleepSeconds(1);
  }

  private void sleepSeconds(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException e) {
      // This is being swallowed on purpose
      Thread.currentThread().interrupt();
      log.error("Exception in thread sleep: ", e);
    }
  }
}
