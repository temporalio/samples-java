package io.temporal.samples.sleepfordays;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

public class Starter {

  public static final String TASK_QUEUE = "SleepForDaysTaskQueue";

  public static void main(String[] args) {
    // Start a workflow execution.
    SleepForDaysWorkflow workflow =
        Worker.client.newWorkflowStub(
            SleepForDaysWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

    // Start the workflow.
    WorkflowClient.start(workflow::sleepForDays);

    WorkflowStub stub = WorkflowStub.fromTyped(workflow);

    // Wait for workflow to complete. This will wait indefinitely until a 'complete' signal is sent.
    stub.getResult(String.class);
    System.exit(0);
  }
}
