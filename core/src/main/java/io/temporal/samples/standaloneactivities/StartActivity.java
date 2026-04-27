package io.temporal.samples.standaloneactivities;

import static io.temporal.samples.standaloneactivities.StandaloneActivityWorker.TASK_QUEUE;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.ActivityHandle;
import io.temporal.client.StartActivityOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;
import java.time.Duration;

/**
 * Starts a standalone activity without blocking, then waits for the result using the returned
 * handle. Requires a Worker running StandaloneActivityWorker.
 */
public class StartActivity {

  static final String ACTIVITY_ID = "standalone-activity-id";

  public static void main(String[] args) throws IOException {
    ClientConfigProfile profile = ClientConfigProfile.load();
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());

    ActivityClient client =
        ActivityClient.newInstance(
            service,
            ActivityClientOptions.newBuilder().setNamespace(profile.getNamespace()).build());

    StartActivityOptions options =
        StartActivityOptions.newBuilder()
            .setId(ACTIVITY_ID)
            .setTaskQueue(TASK_QUEUE)
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .build();

    try {
      ActivityHandle<String> handle =
          client.start(
              GreetingActivities.class,
              GreetingActivities::composeGreeting,
              options,
              "Hello",
              "World");
      System.out.println("Started activity ID: " + ACTIVITY_ID);

      String result = handle.getResult();
      System.out.println("Activity result: " + result);
    } finally {
      service.shutdown();
    }
  }
}
