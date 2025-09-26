package io.temporal.samples.retryonsignalinterceptor;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import java.io.IOException;

public class MyWorkflowWorker {

  static final String TASK_QUEUE = "RetryOnSignalInterceptor";
  static final String WORKFLOW_ID = "RetryOnSignalInterceptor1";

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
    // Register interceptor with the factory.
    WorkerFactoryOptions factoryOptions =
        WorkerFactoryOptions.newBuilder()
            .setWorkerInterceptors(new RetryOnSignalWorkerInterceptor())
            .validateAndBuildWithDefaults();
    WorkerFactory factory = WorkerFactory.newInstance(client, factoryOptions);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(MyWorkflowImpl.class);
    worker.registerActivitiesImplementations(new MyActivityImpl());
    factory.start();

    // Create the workflow client stub. It is used to start our workflow execution.
    MyWorkflow workflow =
        client.newWorkflowStub(
            MyWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    // Execute workflow waiting for it to complete.
    System.out.println("Starting workflow " + WORKFLOW_ID);
    workflow.execute();
    System.out.println("Workflow completed");
    System.exit(0);
  }
}
