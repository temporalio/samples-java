package io.temporal.samples.nexusmessaging.callerpattern.handler;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.envconfig.LoadClientConfigProfileOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerWorker {
  private static final Logger logger = LoggerFactory.getLogger(HandlerWorker.class);

  static final String CONFIG_PROFILE = "nexus-messaging-handler";
  public static final String TASK_QUEUE = "nexus-messaging-handler-task-queue";
  static final String USER_ID = "user-1";

  public static void main(String[] args) throws InterruptedException {
    ClientConfigProfile profile;
    try {
      String configFilePath =
          Paths.get(HandlerWorker.class.getResource("/config.toml").toURI()).toString();
      profile =
          ClientConfigProfile.load(
              LoadClientConfigProfileOptions.newBuilder()
                  .setConfigFilePath(configFilePath)
                  .setConfigFileProfile(CONFIG_PROFILE)
                  .build());
    } catch (Exception e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

    // Start the long-running entity workflow that backs the Nexus service, if not already running.
    // Create a workflow ID derived from the given user ID.
    // This would be for a process that would create a workflow for each UserID,
    // if you had a single long running workflow for all users then you could
    // remove all the USER_IDs from the inputs and just make everything refer
    // to a single workflow ID.
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
