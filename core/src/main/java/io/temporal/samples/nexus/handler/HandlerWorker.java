package io.temporal.samples.nexus.handler;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class HandlerWorker {
  public static final String DEFAULT_TASK_QUEUE_NAME = "my-handler-task-queue";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder().setNamespace("my-target-namespace").build());
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(DEFAULT_TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(HelloHandlerWorkflowImpl.class);
    worker.registerNexusServiceImplementation(new NexusServiceImpl());

    factory.start();
  }
}
