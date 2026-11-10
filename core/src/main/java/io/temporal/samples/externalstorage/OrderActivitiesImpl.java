package io.temporal.samples.externalstorage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderActivitiesImpl implements OrderActivities {

  /**
   * Random notes don't compress well, so the order list stays above the external storage threshold
   * after the codec compresses it.
   */
  private static final int NOTES_UUIDS_PER_ORDER = 100;

  @Override
  public List<Order> fetchOrders(int orderCount) {
    List<Order> orders = new ArrayList<>(orderCount);
    for (int i = 1; i <= orderCount; i++) {
      StringBuilder notes = new StringBuilder();
      for (int j = 0; j < NOTES_UUIDS_PER_ORDER; j++) {
        notes.append(UUID.randomUUID());
      }
      orders.add(new Order(String.format("ORD-%06d", i), i % 50 + 0.5, notes.toString()));
    }
    return orders;
  }

  @Override
  public BatchSummary summarizeOrders(List<Order> orders) {
    double totalWeightKg = orders.stream().mapToDouble(Order::weightKg).sum();
    return new BatchSummary(orders.size(), totalWeightKg);
  }
}
