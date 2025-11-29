package io.temporal.samples.sleepfordays;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

public class Worker {
  public static final String TASK_QUEUE = "SleepForDaysTaskQueue";

  public static void main(String[] args) {
    // Load configuration from environment and files
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
    WorkerFactory factory = WorkerFactory.newInstance(client);

    io.temporal.worker.Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(SleepForDaysImpl.class);
    worker.registerActivitiesImplementations(new SendEmailActivityImpl());

    factory.start();
    System.out.println("Worker started for task queue: " + TASK_QUEUE);
  }
}
