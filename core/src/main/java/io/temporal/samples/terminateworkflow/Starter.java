package io.temporal.samples.terminateworkflow;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Starter {

  public static final String TASK_QUEUE = "terminateQueue";

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

    // Create Worker
    createWorker(factory);

    // Create Workflow options
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setWorkflowId("toTerminateWorkflow")
            .setTaskQueue(TASK_QUEUE)
            .build();

    // Get the Workflow stub
    MyWorkflow myWorkflowStub = client.newWorkflowStub(MyWorkflow.class, workflowOptions);

    // Start workflow async
    WorkflowExecution execution = WorkflowClient.start(myWorkflowStub::execute);

    // Let it run for a couple of seconds
    sleepSeconds(2);

    // Terminate it
    WorkflowStub untyped = WorkflowStub.fromTyped(myWorkflowStub);
    untyped.terminate("Sample reason");

    // Check workflow status, should be WORKFLOW_EXECUTION_STATUS_TERMINATED
    System.out.println("Status: " + getStatusAsString(execution, client, service));

    System.exit(0);
  }

  /** This method creates a Worker from the factory. */
  private static void createWorker(WorkerFactory factory) {
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(MyWorkflowImpl.class);

    factory.start();
  }

  /**
   * Convenience method to sleep for a number of seconds.
   *
   * @param seconds amount of seconds to sleep
   */
  private static void sleepSeconds(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (Exception e) {
      System.out.println("Exception: " + e.getMessage());
      System.exit(0);
    }
  }

  /**
   * This method uses DescribeWorkflowExecutionRequest to get the status of a workflow given a
   * WorkflowExecution.
   *
   * @param execution workflow execution
   * @return Workflow status
   */
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
}
