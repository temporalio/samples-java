package io.temporal.samples.nexusstandalone.service;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;

/**
 * Builds a {@link WorkflowClient} from the {@code default} profile loaded by {@link
 * ClientConfigProfile#load()}. By default this reads the TOML file at {@code TEMPORAL_CONFIG_FILE},
 * or, if that is unset, {@code [user config dir]/temporalio/temporal.toml}. Point that profile at a
 * different server or namespace — or override via {@code TEMPORAL_*} environment variables — to run
 * against, for example, a Temporal Cloud namespace with an API key.
 */
public class ClientOptions {

  public static WorkflowClient getWorkflowClient() {
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (Exception e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    return WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
  }
}
