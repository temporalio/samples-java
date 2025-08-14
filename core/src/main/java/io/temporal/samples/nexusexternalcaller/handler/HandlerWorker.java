package io.temporal.samples.nexusexternalcaller.handler;

import io.temporal.client.WorkflowClient;
import io.temporal.samples.nexus.handler.HelloHandlerWorkflowImpl;
import io.temporal.samples.nexus.handler.NexusServiceImpl;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class HandlerWorker {
  public static final String DEFAULT_TASK_QUEUE_NAME = "my-handler-task-queue";

  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient(args);

    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(DEFAULT_TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(HelloHandlerWorkflowImpl.class);
    worker.registerNexusServiceImplementation(new NexusServiceImpl());

    factory.start();
  }
}
