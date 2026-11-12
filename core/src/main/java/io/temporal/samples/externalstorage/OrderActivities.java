package io.temporal.samples.externalstorage;

import io.temporal.activity.ActivityInterface;
import java.util.List;

@ActivityInterface
public interface OrderActivities {

  List<Order> fetchOrders(int orderCount);

  BatchSummary summarizeOrders(List<Order> orders);
}
