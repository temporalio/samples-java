package io.temporal.samples.getresultsasync;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

public class Worker {
  public static final WorkflowServiceStubs service;
  public static final WorkflowClient client;
  public static final WorkerFactory factory;

  static {
    // Load configuration from environment and files
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    service = WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
    factory = WorkerFactory.newInstance(client);
  }

  public static final String TASK_QUEUE_NAME = "asyncstartqueue";

  public static void main(String[] args) {
    io.temporal.worker.Worker worker = factory.newWorker(TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(MyWorkflowImpl.class);

    factory.start();
  }
}
