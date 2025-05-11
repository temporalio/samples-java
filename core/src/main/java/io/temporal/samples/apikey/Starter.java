package io.temporal.samples.apikey;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class Starter {

  static final String TASK_QUEUE = "MyTaskQueue";
  static final String WORKFLOW_ID = "HelloAPIKeyWorkflow";

  public static void main(String[] args) throws Exception {
    // For temporal cloud this would likely be ${namespace}.tmprl.cloud:7233
    String targetEndpoint = System.getenv("TEMPORAL_ENDPOINT");
    // Your registered namespace.
    String namespace = System.getenv("TEMPORAL_NAMESPACE");
    // Your API Key
    String apiKey = System.getenv("TEMPORAL_API_KEY");

    if (targetEndpoint == null || namespace == null || apiKey == null) {
      throw new IllegalArgumentException(
          "TEMPORAL_ENDPOINT, TEMPORAL_NAMESPACE, and TEMPORAL_API_KEY environment variables must be set");
    }

    // Create API Key enabled client
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(targetEndpoint)
                .setEnableHttps(true)
                .addApiKey(() -> apiKey)
                .build());

    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setNamespace(namespace).build());

    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(TASK_QUEUE);

    worker.registerWorkflowImplementationTypes(MyWorkflowImpl.class);

    factory.start();

    // Create the workflow client stub. It is used to start our workflow execution.
    MyWorkflow workflow =
        client.newWorkflowStub(
            MyWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    String greeting = workflow.execute();

    // Display workflow execution results
    System.out.println(greeting);
    System.exit(0);
  }
}
