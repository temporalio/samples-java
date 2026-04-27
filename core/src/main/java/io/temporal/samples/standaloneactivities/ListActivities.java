package io.temporal.samples.standaloneactivities;

import static io.temporal.samples.standaloneactivities.StandaloneActivityWorker.TASK_QUEUE;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.ActivityExecutionMetadata;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;
import java.util.stream.Stream;

/** Lists standalone activity executions on the task queue. */
public class ListActivities {

  public static void main(String[] args) throws IOException {
    ClientConfigProfile profile = ClientConfigProfile.load();
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());

    ActivityClient client =
        ActivityClient.newInstance(
            service,
            ActivityClientOptions.newBuilder().setNamespace(profile.getNamespace()).build());

    try (Stream<ActivityExecutionMetadata> activities =
        client.listExecutions("TaskQueue = '" + TASK_QUEUE + "'")) {
      activities.forEach(
          info ->
              System.out.printf(
                  "ActivityID: %s, Type: %s, Status: %s%n",
                  info.getActivityId(), info.getActivityType(), info.getStatus()));
    } finally {
      service.shutdown();
    }
  }
}
