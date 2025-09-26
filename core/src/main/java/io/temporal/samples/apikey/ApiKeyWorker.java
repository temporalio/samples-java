package io.temporal.samples.apikey;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

public class ApiKeyWorker {
  static final String TASK_QUEUE = "MyTaskQueue";

  public static void main(String[] args) throws Exception {
    // Load configuration from environment and files
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    // For temporal cloud this would be ${cloud-region}.{cloud}.api.temporal.io:7233
    // Example us-east-1.aws.api.temporal.io:7233
    String targetEndpoint = System.getenv("TEMPORAL_ENDPOINT");
    // Your registered namespace.
    String namespace = System.getenv("TEMPORAL_NAMESPACE");
    // Your API Key
    String apiKey = System.getenv("TEMPORAL_API_KEY");

    if (targetEndpoint == null || namespace == null || apiKey == null) {
      throw new IllegalArgumentException(
          "TEMPORAL_ENDPOINT, TEMPORAL_NAMESPACE, and TEMPORAL_API_KEY environment variables must be set");
    }

    // Create API Key enabled client with environment config as base
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder(profile.toWorkflowServiceStubsOptions())
                .setTarget(targetEndpoint)
                .setEnableHttps(true)
                .addApiKey(() -> apiKey)
                .build());

    // Now setup and start workflow worker
    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder(profile.toWorkflowClientOptions())
                .setNamespace(namespace)
                .build());

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);

    /*
     * Define the workflow worker. Workflow workers listen to a defined task queue and process
     * workflows and activities.
     */
    Worker worker = factory.newWorker(TASK_QUEUE);

    /*
     * Register our workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(MyWorkflowImpl.class);

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    System.out.println("Worker started. Press Ctrl+C to exit.");
    // Keep the worker running
    Thread.currentThread().join();
  }
}
