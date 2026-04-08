package io.temporal.samples.nexus_sync_operations.caller_remote;

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

public class CallerRemoteWorker {
  private static final Logger logger = LoggerFactory.getLogger(CallerRemoteWorker.class);

  public static final String NAMESPACE = "nexus-sync-operations-caller-namespace";
  public static final String TASK_QUEUE = "nexus-sync-operations-caller-remote-task-queue";
  static final String NEXUS_ENDPOINT = "nexus-sync-operations-nexus-endpoint";

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
                Collections.singletonMap(
                    "NexusRemoteGreetingService",
                    NexusServiceOptions.newBuilder().setEndpoint(NEXUS_ENDPOINT).build()))
            .build(),
        CallerRemoteWorkflowImpl.class);

    factory.start();
    logger.info("Caller remote worker started, ctrl+c to exit");
    Thread.currentThread().join();
  }
}
