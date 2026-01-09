package io.temporal.samples.envconfig;

// @@@SNIPSTART java-env-config-profile-with-overrides
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
 * This sample demonstrates loading a specific profile from a TOML configuration file with
 * programmatic overrides.
 */
public class LoadProfile {

  private static final Logger logger = LoggerFactory.getLogger(LoadProfile.class);

  public static void main(String[] args) {
    String profileName = "staging";

    try {
      // For this sample to be self-contained, we explicitly provide the path to
      // the config.toml file included in this directory.
      String configFilePath =
          Paths.get(LoadProfile.class.getResource("/config.toml").toURI()).toString();

      logger.info("--- Loading '{}' profile from {} ---", profileName, configFilePath);

      // Load specific profile from file with environment variable overrides
      ClientConfigProfile profile =
          ClientConfigProfile.load(
              LoadClientConfigProfileOptions.newBuilder()
                  .setConfigFilePath(configFilePath)
                  .setConfigFileProfile(profileName)
                  .build());

      // Demonstrate programmatic override - fix the incorrect address from staging profile
      logger.info("\n--- Applying programmatic override ---");
      ClientConfigProfile.Builder profileBuilder = profile.toBuilder();
      profileBuilder.setAddress("localhost:7233"); // Override the incorrect address
      profile = profileBuilder.build();
      logger.info("  Overridden address to: {}", profile.getAddress());

      // Convert profile to client options (equivalent to Python's load_client_connect_config)
      WorkflowServiceStubsOptions serviceStubsOptions = profile.toWorkflowServiceStubsOptions();
      WorkflowClientOptions clientOptions = profile.toWorkflowClientOptions();

      logger.info("Loaded '{}' profile from {}", profileName, configFilePath);
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
// @@@SNIPEND
