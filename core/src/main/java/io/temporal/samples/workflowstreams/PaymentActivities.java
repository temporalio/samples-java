package io.temporal.samples.workflowstreams;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface PaymentActivities {
  /**
   * Charges a card and publishes fine-grained progress events back to its parent workflow's stream.
   */
  @ActivityMethod
  String chargeCard(String orderId);
}
