package io.temporal.samples.nexusmessaging.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.samples.nexusmessaging.options.ClientOptions;
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
                    "SampleNexusService",
                    NexusServiceOptions.newBuilder().setEndpoint("my-nexus-endpoint-name").build()))
            .build(),
        // MessageCallerWorkflowImpl.class,
        MessageCallerStartHandlerWorkflowImpl.class);

    factory.start();
  }
}
