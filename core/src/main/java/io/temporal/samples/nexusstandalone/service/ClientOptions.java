package io.temporal.samples.nexusstandalone.service;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.envconfig.LoadClientConfigProfileOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.nio.file.Paths;

/**
 * Builds a {@link WorkflowClient} from the {@code default} profile in {@code
 * core/src/main/resources/config.toml}. Edit that profile (or override via {@code TEMPORAL_*}
 * environment variables) to point at a different server or namespace — for example a Temporal Cloud
 * namespace with an API key.
 */
public class ClientOptions {

  public static WorkflowClient getWorkflowClient() {
    ClientConfigProfile profile;
    try {
      String configFilePath =
          Paths.get(ClientOptions.class.getResource("/config.toml").toURI()).toString();
      profile =
          ClientConfigProfile.load(
              LoadClientConfigProfileOptions.newBuilder()
                  .setConfigFilePath(configFilePath)
                  .build());
    } catch (Exception e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    return WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
  }
}
