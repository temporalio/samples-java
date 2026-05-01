package io.temporal.samples.standaloneactivities;

import static io.temporal.samples.standaloneactivities.StandaloneActivityWorker.TASK_QUEUE;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.ActivityExecutionCount;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;

/** Counts standalone activity executions on the task queue. */
public class CountActivities {

  public static void main(String[] args) throws IOException {
    ClientConfigProfile profile = ClientConfigProfile.load();
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());

    ActivityClient client =
        ActivityClient.newInstance(
            service,
            ActivityClientOptions.newBuilder().setNamespace(profile.getNamespace()).build());

    try {
      ActivityExecutionCount resp = client.countExecutions("TaskQueue = '" + TASK_QUEUE + "'");

      System.out.println("Total activities: " + resp.getCount());
      resp.getGroups()
          .forEach(
              group ->
                  System.out.println("Group " + group.getGroupValues() + ": " + group.getCount()));
    } finally {
      service.shutdown();
    }
  }
}
