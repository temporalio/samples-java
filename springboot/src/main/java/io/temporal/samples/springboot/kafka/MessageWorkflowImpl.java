package io.temporal.samples.springboot.kafka;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import java.time.Duration;

@WorkflowImpl(taskQueues = "KafkaSampleTaskQueue")
public class MessageWorkflowImpl implements MessageWorkflow {

  private KafkaActivity activity =
      Workflow.newActivityStub(
          KafkaActivity.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());
  private String message = null;

  @Override
  public void start() {
    Workflow.await(() -> message != null);
    // simulate some steps / milestones
    activity.sendMessage(
        "Starting execution: " + Workflow.getInfo().getWorkflowId() + " with message: " + message);

    activity.sendMessage("Step 1 done...");
    activity.sendMessage("Step 2 done...");
    activity.sendMessage("Step 3 done...");

    activity.sendMessage("Completing execution: " + Workflow.getInfo().getWorkflowId());
  }

  @Override
  public void update(String message) {
    this.message = message;
  }
}
