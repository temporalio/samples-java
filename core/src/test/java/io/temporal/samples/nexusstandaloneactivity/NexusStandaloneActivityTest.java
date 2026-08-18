package io.temporal.samples.nexusstandaloneactivity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.api.nexus.v1.Endpoint;
import io.temporal.client.NexusClient;
import io.temporal.client.NexusClientOptions;
import io.temporal.client.NexusServiceClient;
import io.temporal.client.StartNexusOperationOptions;
import io.temporal.samples.nexusstandaloneactivity.handler.GreetingActivityImpl;
import io.temporal.samples.nexusstandaloneactivity.handler.GreetingNexusServiceImpl;
import io.temporal.samples.nexusstandaloneactivity.handler.HandlerWorker;
import io.temporal.samples.nexusstandaloneactivity.service.GreetingNexusService;
import io.temporal.testing.TemporalDevServerOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class NexusStandaloneActivityTest {
  private static final String DEV_SERVER_VERSION = "v1.7.4-standalone-nexus-operations";

  private static TestWorkflowEnvironment testEnv;
  private static Endpoint endpoint;

  @BeforeAll
  public static void setUp() {
    testEnv =
        TestWorkflowEnvironment.startLocal(
            TemporalDevServerOptions.newBuilder()
                .setDownloadVersion(DEV_SERVER_VERSION)
                .setExtraArgs("--dynamic-config-value", "activity.enableCallbacks=true")
                .build());

    endpoint =
        testEnv.createNexusEndpoint(
            "test-nexus-endpoint-" + UUID.randomUUID(), HandlerWorker.TASK_QUEUE_NAME);

    Worker worker = testEnv.newWorker(HandlerWorker.TASK_QUEUE_NAME);
    worker.registerActivitiesImplementations(new GreetingActivityImpl());
    worker.registerNexusServiceImplementation(new GreetingNexusServiceImpl());
    testEnv.start();
  }

  @AfterAll
  public static void tearDown() {
    if (testEnv != null) {
      if (endpoint != null) {
        testEnv.deleteNexusEndpoint(endpoint);
      }
      testEnv.close();
    }
  }

  @Test
  public void testNexusOperationBackedByStandaloneActivity() {
    NexusClient nexusClient =
        NexusClient.newInstance(
            testEnv.getWorkflowServiceStubs(),
            NexusClientOptions.newBuilder().setNamespace(testEnv.getNamespace()).build());
    NexusServiceClient<GreetingNexusService> greetingClient =
        nexusClient.newNexusServiceClient(GreetingNexusService.class, endpoint.getSpec().getName());

    GreetingNexusService.GreetingOutput output =
        greetingClient.execute(
            GreetingNexusService::greet,
            StartNexusOperationOptions.newBuilder()
                .setId("greeting-" + UUID.randomUUID())
                .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                .build(),
            new GreetingNexusService.GreetingInput("Test"));

    assertEquals("Hello, Test!", output.getMessage());
  }
}
