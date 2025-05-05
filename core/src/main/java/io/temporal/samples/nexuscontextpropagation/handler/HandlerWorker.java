package io.temporal.samples.nexuscontextpropagation.handler;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.samples.nexuscontextpropagation.propagation.MDCContextPropagator;
import io.temporal.samples.nexuscontextpropagation.propagation.NexusMDCContextInterceptor;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import java.util.Collections;

public class HandlerWorker {
  public static final String DEFAULT_TASK_QUEUE_NAME = "my-handler-task-queue";

  public static void main(String[] args) {
    WorkflowClient client =
        ClientOptions.getWorkflowClient(
            args,
            WorkflowClientOptions.newBuilder()
                .setContextPropagators(Collections.singletonList(new MDCContextPropagator())));

    WorkerFactory factory =
        WorkerFactory.newInstance(
            client,
            WorkerFactoryOptions.newBuilder()
                .setWorkerInterceptors(new NexusMDCContextInterceptor())
                .build());

    Worker worker = factory.newWorker(DEFAULT_TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(HelloHandlerWorkflowImpl.class);
    worker.registerNexusServiceImplementation(new NexusServiceImpl());

    factory.start();
  }
}
