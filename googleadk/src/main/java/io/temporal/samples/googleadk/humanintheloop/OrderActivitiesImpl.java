package io.temporal.samples.googleadk.humanintheloop;

import java.util.Map;

public class OrderActivitiesImpl implements OrderActivities {
  @Override
  public Map<String, Object> shipOrder(String orderId) {
    // No order is actually shipped. A real integration would call the fulfillment API here.
    return Map.of("orderId", orderId, "status", "shipped");
  }
}
