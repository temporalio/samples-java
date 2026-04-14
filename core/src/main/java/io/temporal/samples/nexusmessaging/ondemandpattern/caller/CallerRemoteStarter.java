package io.temporal.samples.nexusmessaging.ondemandpattern.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.envconfig.LoadClientConfigProfileOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class CallerRemoteStarter {

  public static void main(String[] args) {
    ClientConfigProfile profile;
    try {
      String configFilePath =
          Paths.get(CallerRemoteStarter.class.getResource("/config.toml").toURI()).toString();
      profile =
          ClientConfigProfile.load(
              LoadClientConfigProfileOptions.newBuilder()
                  .setConfigFilePath(configFilePath)
                  .setConfigFileProfile(CallerRemoteWorker.CONFIG_PROFILE)
                  .build());
    } catch (Exception e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

    CallerRemoteWorkflow workflow =
        client.newWorkflowStub(
            CallerRemoteWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("nexus-messaging-remote-caller-" + UUID.randomUUID())
                .setTaskQueue(CallerRemoteWorker.TASK_QUEUE)
                .build());

    List<String> log = workflow.run();
    log.forEach(System.out::println);
  }
}
