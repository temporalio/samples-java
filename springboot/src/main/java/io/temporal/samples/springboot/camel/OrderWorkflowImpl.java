package io.temporal.samples.springboot.camel;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.List;

@WorkflowImpl(taskQueues = "CamelSampleTaskQueue")
public class OrderWorkflowImpl implements OrderWorkflow {

  private OrderActivity activity =
      Workflow.newActivityStub(
          OrderActivity.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

  @Override
  public List<OfficeOrder> start() {
    return activity.getOrders();
  }
}
