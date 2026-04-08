package io.temporal.samples.nexusmessaging.handler;

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

  public static final String NAMESPACE = "my-target-namespace";
  public static final String DEFAULT_TASK_QUEUE_NAME = "my-handler-task-queue";
  static final String WORKFLOW_ID = "my-handler-workflow";

  private static final Logger logger = LoggerFactory.getLogger(HandlerWorker.class);

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setNamespace(NAMESPACE).build());

    // Start the long-running entity workflow that backs the Nexus service, if not already running.
    MessageHandlerWorkflow messageHandlerWorkflow =
        client.newWorkflowStub(
            MessageHandlerWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(DEFAULT_TASK_QUEUE_NAME)
                .build());
    try {
      WorkflowClient.start(messageHandlerWorkflow::run);
      logger.info("Started message handler workflow: {}", WORKFLOW_ID);
    } catch (WorkflowExecutionAlreadyStarted e) {
      logger.info("Message handler workflow already running: {}", WORKFLOW_ID);
    }

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(DEFAULT_TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(
        MessageHandlerWorkflowImpl.class, MessageHandlerRemoteWorkflowImpl.class);
    //    worker.registerActivitiesImplementations(new MessageHandlerWorkflowImpl());
    worker.registerNexusServiceImplementation(new SampleNexusServiceImpl(WORKFLOW_ID));

    factory.start();
    logger.info("Handler worker started, ctrl+c to exit");

    //    WorkflowClient client = ClientOptions.getWorkflowClient(args);
    //
    //    WorkerFactory factory = WorkerFactory.newInstance(client);
    //
    //    Worker worker = factory.newWorker(DEFAULT_TASK_QUEUE_NAME);
    //    worker.registerWorkflowImplementationTypes(MessageHandlerWorkflowImpl.class);
    //    worker.registerNexusServiceImplementation(new SampleNexusServiceImpl());
    //
    //    factory.start();
  }
}
