package io.temporal.samples.nexusmessaging.ondemandpattern.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.envconfig.LoadClientConfigProfileOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;
import java.nio.file.Paths;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallerRemoteWorker {
  private static final Logger logger = LoggerFactory.getLogger(CallerRemoteWorker.class);

  static final String CONFIG_PROFILE = "nexus-messaging-caller";
  public static final String TASK_QUEUE = "nexus-messaging-caller-remote-task-queue";
  static final String NEXUS_ENDPOINT = "nexus-messaging-nexus-endpoint";

  public static void main(String[] args) throws InterruptedException {
    ClientConfigProfile profile;
    try {
      String configFilePath =
          Paths.get(CallerRemoteWorker.class.getResource("/config.toml").toURI()).toString();
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
    worker.registerWorkflowImplementationTypes(
        WorkflowImplementationOptions.newBuilder()
            .setNexusServiceOptions(
                // The key must match the @Service-annotated interface name.
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
