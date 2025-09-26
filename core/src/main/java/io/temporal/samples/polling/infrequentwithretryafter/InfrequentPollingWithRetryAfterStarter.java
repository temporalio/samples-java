package io.temporal.samples.polling.infrequentwithretryafter;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.samples.polling.PollingWorkflow;
import io.temporal.samples.polling.TestService;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

public class InfrequentPollingWithRetryAfterStarter {
  private static WorkflowServiceStubs service;
  private static WorkflowClient client;

  static {
    // Load configuration from environment and files
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    service = WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
  }

  private static final String taskQueue = "pollingSampleQueue";
  private static final String workflowId = "InfrequentPollingWithRetryAfterWorkflow";

  public static void main(String[] args) {
    // Create our worker and register workflow and activities
    createWorker();

    // Create typed workflow stub and start execution (sync, wait for results)
    PollingWorkflow workflow =
        client.newWorkflowStub(
            PollingWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(taskQueue).setWorkflowId(workflowId).build());
    String result = workflow.exec();
    System.out.println("Result: " + result);
    System.exit(0);
  }

  private static void createWorker() {
    WorkerFactory workerFactory = WorkerFactory.newInstance(client);
    Worker worker = workerFactory.newWorker(taskQueue);

    // Register workflow and activities
    worker.registerWorkflowImplementationTypes(InfrequentPollingWithRetryAfterWorkflowImpl.class);
    worker.registerActivitiesImplementations(
        new InfrequentPollingWithRetryAfterActivityImpl(new TestService(4, true)));

    workerFactory.start();
  }
}
