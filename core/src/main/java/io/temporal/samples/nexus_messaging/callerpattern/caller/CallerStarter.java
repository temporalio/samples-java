package io.temporal.samples.nexus_messaging.callerpattern.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.envconfig.LoadClientConfigProfileOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class CallerStarter {

  public static void main(String[] args) {
    ClientConfigProfile profile;
    try {
      String configFilePath =
          Paths.get(CallerStarter.class.getResource("/config.toml").toURI()).toString();
      profile =
          ClientConfigProfile.load(
              LoadClientConfigProfileOptions.newBuilder()
                  .setConfigFilePath(configFilePath)
                  .setConfigFileProfile(CallerWorker.CONFIG_PROFILE)
                  .build());
    } catch (Exception e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

    CallerWorkflow workflow =
        client.newWorkflowStub(
            CallerWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("nexus-messaging-caller-" + UUID.randomUUID())
                .setTaskQueue(CallerWorker.TASK_QUEUE)
                .build());

    // Launch the worker, passing in an identifier which the Nexus service will use
    // to find the matching workflow  (See NexusGreetingServiceImpl::getWorkflowId)
    List<String> log = workflow.run("user-1");
    log.forEach(System.out::println);
  }
}
