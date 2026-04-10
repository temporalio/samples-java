package io.temporal.samples.nexus_messaging.callerpattern.handler;

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
  static final String USER_ID = "user-1";

  public static void main(String[] args) throws InterruptedException {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setNamespace(NAMESPACE).build());

    // Start the long-running entity workflow that backs the Nexus service, if not already running.
    // The workflow ID is derived from the user ID using the same prefix as
    // NexusGreetingServiceImpl.
    String workflowId = NexusGreetingServiceImpl.getWorkflowId(USER_ID);
    GreetingWorkflow greetingWorkflow =
        client.newWorkflowStub(
            GreetingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(TASK_QUEUE)
                .build());
    try {
      WorkflowClient.start(greetingWorkflow::run);
      logger.info("Started greeting workflow: {}", workflowId);
    } catch (WorkflowExecutionAlreadyStarted e) {
      logger.info("Greeting workflow already running: {}", workflowId);
    }

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    worker.registerActivitiesImplementations(new GreetingActivityImpl());
    worker.registerNexusServiceImplementation(new NexusGreetingServiceImpl());

    factory.start();
    logger.info("Handler worker started, ctrl+c to exit");
    Thread.currentThread().join();
  }
}
