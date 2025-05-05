package io.temporal.samples.nexuscancellation.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.samples.nexus.service.NexusService;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;
import java.util.Collections;

public class CallerWorker {
  public static final String DEFAULT_TASK_QUEUE_NAME = "my-caller-workflow-task-queue";

  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient(args);

    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(DEFAULT_TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(
        WorkflowImplementationOptions.newBuilder()
            .setNexusServiceOptions(
                Collections.singletonMap(
                    NexusService.class.getSimpleName(),
                    NexusServiceOptions.newBuilder().setEndpoint("my-nexus-endpoint-name").build()))
            .build(),
        HelloCallerWorkflowImpl.class);

    factory.start();
  }
}
