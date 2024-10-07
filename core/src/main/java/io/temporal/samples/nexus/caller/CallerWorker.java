package io.temporal.samples.nexus.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;

public class CallerWorker {
  public static final String DEFAULT_TASK_QUEUE_NAME = "my-caller-workflow-task-queue";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder().setNamespace("my-caller-namespace").build());
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(DEFAULT_TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(
        WorkflowImplementationOptions.newBuilder()
            .setDefaultNexusServiceOptions(
                NexusServiceOptions.newBuilder().setEndpoint("my-nexus-endpoint-name").build())
            .build(),
        EchoCallerWorkflowImpl.class,
        HelloCallerWorkflowImpl.class);

    factory.start();
  }
}
