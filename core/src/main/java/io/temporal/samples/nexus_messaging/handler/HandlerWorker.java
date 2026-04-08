package io.temporal.samples.nexus_messaging.handler;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerWorker {
  private static final Logger logger = LoggerFactory.getLogger(HandlerWorker.class);

  public static final String NAMESPACE = "nexus-messaging-handler-namespace";
  public static final String TASK_QUEUE = "nexus-messaging-handler-task-queue";
  static final String WORKFLOW_ID = "nexus-messaging-greeting-workflow";

  public static void main(String[] args) throws InterruptedException {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setNamespace(NAMESPACE).build());

    // Start the long-running entity workflow that backs the Nexus service, if not already running.
    GreetingWorkflow greetingWorkflow =
        client.newWorkflowStub(
            GreetingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());
    try {
      WorkflowClient.start(greetingWorkflow::run);
      logger.info("Started greeting workflow: {}", WORKFLOW_ID);
    } catch (WorkflowExecutionAlreadyStarted e) {
      logger.info("Greeting workflow already running: {}", WORKFLOW_ID);
    }

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    worker.registerActivitiesImplementations(new GreetingActivityImpl());
    worker.registerNexusServiceImplementation(new NexusGreetingServiceImpl(WORKFLOW_ID));
    worker.registerNexusServiceImplementation(new NexusRemoteGreetingServiceImpl());

    factory.start();
    logger.info("Handler worker started, ctrl+c to exit");
    Thread.currentThread().join();
  }
}
