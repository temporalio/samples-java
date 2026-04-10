package io.temporal.samples.nexus_messaging.callerpattern.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallerWorker {
  private static final Logger logger = LoggerFactory.getLogger(CallerWorker.class);

  public static final String NAMESPACE = "nexus-messaging-caller-namespace";
  public static final String TASK_QUEUE = "nexus-messaging-caller-task-queue";
  static final String NEXUS_ENDPOINT = "nexus-messaging-nexus-endpoint";

  public static void main(String[] args) throws InterruptedException {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setNamespace(NAMESPACE).build());

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(
        WorkflowImplementationOptions.newBuilder()
            .setNexusServiceOptions(
                // The key must match the @Service-annotated interface name.
                Collections.singletonMap(
                    "NexusGreetingService",
                    NexusServiceOptions.newBuilder().setEndpoint(NEXUS_ENDPOINT).build()))
            .build(),
        CallerWorkflowImpl.class);

    factory.start();
    logger.info("Caller worker started, ctrl+c to exit");
    Thread.currentThread().join();
  }
}
