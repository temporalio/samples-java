package io.temporal.samples.helloworld;
// @@@SNIPSTART java-hello-world-sample-worker
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class HelloWorldWorker {

  public static void main(String args[]) {
    // Create a gRPC stubs wrapper that talks to the local docker instance of the Temporal service
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    // Create a Temporal Java SDK client
    WorkflowClient client = WorkflowClient.newInstance(service);
    // Create a Worker factory that can be used to create Workers for specific Task Queues
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // Define the name of the Task Queue that the Worker will listen to
    final String TASK_QUEUE = "java-hello-world-task-queue";
    // Create a Worker that listens on a Task Queue
    Worker worker = factory.newWorker(TASK_QUEUE);
    // This Worker hosts both Workflow and Activity implementations
    // Register the Workflow with the Worker
    // Workflows are stateful. So you need a type to create instances.
    worker.registerWorkflowImplementationTypes(HelloWorldWorkflowImpl.class);
    // Register the Activity with the Worker
    // Activities are stateless and thread safe, so a shared instance is used.
    worker.registerActivitiesImplementations(new HelloWorldActivityImpl());
    // Start listening to the Task Queue.
    factory.start();
  }
}
// @@@SNIPEND
