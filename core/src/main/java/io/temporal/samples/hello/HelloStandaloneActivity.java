package io.temporal.samples.hello;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.StartActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample Temporal application that executes a Standalone Activity — an Activity that runs
 * independently, without being orchestrated by a Workflow. Requires a local instance of the
 * Temporal service to be running.
 *
 * <p>Unlike regular Activities, a Standalone Activity is started directly from a Temporal Client
 * using {@link ActivityClient}, not from inside a Workflow Definition. Writing the Activity and
 * registering it with the Worker is identical in both cases.
 */
public class HelloStandaloneActivity {

  static final String TASK_QUEUE = "HelloStandaloneActivityTaskQueue";
  static final String ACTIVITY_ID = "hello-standalone-activity-id";

  /**
   * Activity interface. Writing a Standalone Activity is identical to writing an Activity
   * orchestrated by a Workflow — the same Activity can be used for both.
   *
   * @see io.temporal.activity.ActivityInterface
   * @see io.temporal.activity.ActivityMethod
   */
  @ActivityInterface
  public interface GreetingActivities {

    // Define your activity method which can be called directly from a Temporal Client.
    @ActivityMethod
    String composeGreeting(String greeting, String name);
  }

  /** Simple activity implementation that concatenates two strings. */
  public static class GreetingActivitiesImpl implements GreetingActivities {

    private static final Logger log = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

    @Override
    public String composeGreeting(String greeting, String name) {
      log.info("Composing greeting...");
      return greeting + ", " + name + "!";
    }
  }

  public static void main(String[] args) {
    // Load configuration from environment and files.
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    // gRPC stubs wrapper that talks to the temporal service.
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());

    // WorkflowClient is required to create a Worker.
    WorkflowClient workflowClient =
        WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

    // Worker factory that can be used to create workers for specific task queues.
    WorkerFactory factory = WorkerFactory.newInstance(workflowClient);

    // Worker that listens on a task queue and hosts activity implementations.
    Worker worker = factory.newWorker(TASK_QUEUE);

    // Activities are stateless and thread safe. So a shared instance is used.
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    // Start listening to the activity task queue.
    factory.start();

    // ActivityClient executes standalone activities directly from application code,
    // without a Workflow.
    ActivityClient client =
        ActivityClient.newInstance(
            service,
            ActivityClientOptions.newBuilder().setNamespace(profile.getNamespace()).build());

    // Options specifying the activity ID, task queue, and timeout.
    StartActivityOptions options =
        StartActivityOptions.newBuilder()
            .setId(ACTIVITY_ID)
            .setTaskQueue(TASK_QUEUE)
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .build();

    try {
      // Execute the activity and wait for its result. The typed API uses an unbound method
      // reference so the SDK can infer the activity type name and result type automatically.
      String result =
          client.execute(
              GreetingActivities.class,
              GreetingActivities::composeGreeting,
              options,
              "Hello",
              "World");

      System.out.println(result);
    } finally {
      // Shut down the worker before the service so polling threads stop cleanly.
      factory.shutdown();
      service.shutdown();
    }
  }
}
