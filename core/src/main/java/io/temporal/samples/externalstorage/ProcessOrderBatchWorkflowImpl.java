package io.temporal.samples.externalstorage;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.List;

public class ProcessOrderBatchWorkflowImpl implements ProcessOrderBatchWorkflow {

  private final OrderActivities activities =
      Workflow.newActivityStub(
          OrderActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(60)).build());

  @Override
  public BatchSummary run(int orderCount) {
    List<Order> orders = activities.fetchOrders(orderCount);
    return activities.summarizeOrders(orders);
  }
}
