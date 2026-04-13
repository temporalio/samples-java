package io.temporal.samples.nexus_messaging.ondemandpattern.handler;

import io.temporal.client.WorkflowClient;
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

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    worker.registerActivitiesImplementations(new GreetingActivityImpl());
    worker.registerNexusServiceImplementation(new NexusRemoteGreetingServiceImpl());

    factory.start();
    logger.info("Handler worker started, ctrl+c to exit");
    Thread.currentThread().join();
  }
}
