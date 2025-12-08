package io.temporal.samples.envconfig;
/** @@@SNIPSTART java-env-config-profile */
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.envconfig.LoadClientConfigProfileOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This sample demonstrates loading the default environment configuration profile from a TOML file.
 */
public class LoadFromFile {

  private static final Logger logger = LoggerFactory.getLogger(LoadFromFile.class);

  public static void main(String[] args) {
    try {
      // For this sample to be self-contained, we explicitly provide the path to
      // the config.toml file included in this directory.
      // By default though, the config.toml file will be loaded from
      // ~/.config/temporal/temporal.toml (or the equivalent standard config directory on your OS).
      String configFilePath =
          Paths.get(LoadFromFile.class.getResource("/config.toml").toURI()).toString();

      logger.info("--- Loading 'default' profile from {} ---", configFilePath);

      // Load client profile from file. By default, this loads the "default" profile
      // and applies any environment variable overrides.
      ClientConfigProfile profile =
          ClientConfigProfile.load(
              LoadClientConfigProfileOptions.newBuilder()
                  .setConfigFilePath(configFilePath)
                  .build());

      // Convert profile to client options (equivalent to Python's load_client_connect_config)
      WorkflowServiceStubsOptions serviceStubsOptions = profile.toWorkflowServiceStubsOptions();
      WorkflowClientOptions clientOptions = profile.toWorkflowClientOptions();

      logger.info("Loaded 'default' profile from {}", configFilePath);
      logger.info("  Address: {}", serviceStubsOptions.getTarget());
      logger.info("  Namespace: {}", clientOptions.getNamespace());
      if (serviceStubsOptions.getHeaders() != null
          && !serviceStubsOptions.getHeaders().keys().isEmpty()) {
        logger.info("  gRPC Metadata keys: {}", serviceStubsOptions.getHeaders().keys());
      }

      logger.info("\nAttempting to connect to client...");

      try {
        // Create the workflow client using the loaded configuration
        WorkflowClient client =
            WorkflowClient.newInstance(
                WorkflowServiceStubs.newServiceStubs(serviceStubsOptions), clientOptions);

        // Test the connection by getting system info
        var systemInfo =
            client
                .getWorkflowServiceStubs()
                .blockingStub()
                .getSystemInfo(
                    io.temporal.api.workflowservice.v1.GetSystemInfoRequest.getDefaultInstance());

        logger.info("✅ Client connected successfully!");
        logger.info("   Server version: {}", systemInfo.getServerVersion());

      } catch (Exception e) {
        logger.error("❌ Failed to connect: {}", e.getMessage());
      }

    } catch (Exception e) {
      logger.error("Failed to load configuration: {}", e.getMessage(), e);
      System.exit(1);
    }
  }
}
/** @@@SNIPEND */