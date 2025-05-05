package io.temporal.samples.listworkflows;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface CustomerActivities {
  void getCustomerAccount(Customer customer);

  void updateCustomerAccount(Customer customer, String message);

  void sendUpdateEmail(Customer customer);
}
