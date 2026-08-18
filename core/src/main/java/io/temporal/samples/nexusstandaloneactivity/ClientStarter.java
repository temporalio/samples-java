package io.temporal.samples.nexusstandaloneactivity;

import io.temporal.client.NexusClient;
import io.temporal.client.NexusClientOptions;
import io.temporal.client.NexusServiceClient;
import io.temporal.client.StartNexusOperationOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.samples.nexusstandaloneactivity.service.ClientOptions;
import io.temporal.samples.nexusstandaloneactivity.service.GreetingNexusService;
import io.temporal.samples.nexusstandaloneactivity.service.GreetingNexusService.GreetingInput;
import io.temporal.samples.nexusstandaloneactivity.service.GreetingNexusService.GreetingOutput;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Executes the Activity-backed Nexus operation from client code. The operation is standalone: it is
// started directly by this client rather than from within a caller Workflow.
public class ClientStarter {
  private static final Logger logger = LoggerFactory.getLogger(ClientStarter.class);

  // Must match the Nexus endpoint configured on the server (see README).
  public static final String ENDPOINT_NAME = "my-nexus-endpoint";

  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient();
    WorkflowServiceStubs stubs = client.getWorkflowServiceStubs();
    String namespace = client.getOptions().getNamespace();

    NexusClient nexusClient =
        NexusClient.newInstance(
            stubs, NexusClientOptions.newBuilder().setNamespace(namespace).build());
    // Typed service client: dispatches operations by method reference on the service interface.
    NexusServiceClient<GreetingNexusService> greetingClient =
        nexusClient.newNexusServiceClient(GreetingNexusService.class, ENDPOINT_NAME);

    // execute() starts the operation and blocks until it completes. The handler backs the operation
    // with a standalone Activity, so this returns once that Activity has produced its result.
    GreetingOutput result =
        greetingClient.execute(
            GreetingNexusService::greet,
            StartNexusOperationOptions.newBuilder()
                .setId("greeting-" + UUID.randomUUID())
                .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                .build(),
            new GreetingInput("World"));

    logger.info(result.getMessage());
  }
}
