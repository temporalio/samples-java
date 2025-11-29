package io.temporal.samples.sleepfordays;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;

public class Starter {

  public static final String TASK_QUEUE = "SleepForDaysTaskQueue";

  public static void main(String[] args) {
    // Load configuration from environment and files
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

    // Start a workflow execution.
    SleepForDaysWorkflow workflow =
        client.newWorkflowStub(
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
