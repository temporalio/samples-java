package io.temporal.samples.standaloneactivities;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

/**
 * Worker that processes standalone activity tasks. Run this before executing or starting standalone
 * activities with ExecuteActivity or StartActivity.
 */
public class StandaloneActivityWorker {

  static final String TASK_QUEUE = "standalone-activity-task-queue";

  public static void main(String[] args) throws IOException {
    ClientConfigProfile profile = ClientConfigProfile.load();
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    factory.start();
    System.out.println("Worker running on task queue: " + TASK_QUEUE);
  }
}
