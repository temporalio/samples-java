package io.temporal.samples.asyncchild;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Starter {

  public static final String TASK_QUEUE = "asyncChildTaskQueue";

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
    WorkerFactory factory = WorkerFactory.newInstance(client);

    createWorker(factory);

    WorkflowOptions parentWorkflowOptions =
        WorkflowOptions.newBuilder()
            .setWorkflowId("parentWorkflow")
            .setTaskQueue(TASK_QUEUE)
            .build();
    ParentWorkflow parentWorkflowStub =
        client.newWorkflowStub(ParentWorkflow.class, parentWorkflowOptions);

    // Start parent workflow and wait for it to complete
    WorkflowExecution childWorkflowExecution = parentWorkflowStub.executeParent();

    // Get the child workflow execution status (after parent completed)
    System.out.println(
        "Child execution status: " + getStatusAsString(childWorkflowExecution, client, service));

    // Wait for child workflow to complete
    sleep(4);

    // Check the status of the child workflow again
    System.out.println(
        "Child execution status: " + getStatusAsString(childWorkflowExecution, client, service));

    System.exit(0);
  }

  private static void createWorker(WorkerFactory factory) {
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(ParentWorkflowImpl.class, ChildWorkflowImpl.class);

    factory.start();
  }

  private static String getStatusAsString(
      WorkflowExecution execution, WorkflowClient client, WorkflowServiceStubs service) {
    DescribeWorkflowExecutionRequest describeWorkflowExecutionRequest =
        DescribeWorkflowExecutionRequest.newBuilder()
            .setNamespace(client.getOptions().getNamespace())
            .setExecution(execution)
            .build();

    DescribeWorkflowExecutionResponse resp =
        service.blockingStub().describeWorkflowExecution(describeWorkflowExecutionRequest);

    WorkflowExecutionInfo workflowExecutionInfo = resp.getWorkflowExecutionInfo();
    return workflowExecutionInfo.getStatus().toString();
  }

  private static void sleep(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (Exception e) {
      System.out.println("Exception: " + e.getMessage());
      System.exit(0);
    }
  }
}
