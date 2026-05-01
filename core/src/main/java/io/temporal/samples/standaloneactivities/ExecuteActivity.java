package io.temporal.samples.standaloneactivities;

import static io.temporal.samples.standaloneactivities.StandaloneActivityWorker.TASK_QUEUE;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.StartActivityOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;
import java.time.Duration;

/**
 * Executes a standalone activity and waits for the result. Requires a Worker running
 * StandaloneActivityWorker.
 */
public class ExecuteActivity {

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
      String result =
          client.execute(
              GreetingActivities.class,
              GreetingActivities::composeGreeting,
              options,
              "Hello",
              "World");
      System.out.println("Activity result: " + result);
    } finally {
      service.shutdown();
    }
  }
}
