package io.temporal.samples.moneytransfer;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

public class AccountActivityWorker {

  public static final String TASK_QUEUE = "AccountTransfer";

  @SuppressWarnings("CatchAndPrintStackTrace")
  public static void main(String[] args) {
    // Load configuration from environment and files
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    // gRPC stubs wrapper that talks to the temporal service.
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    Account account = new AccountImpl();
    worker.registerActivitiesImplementations(account);

    // Start all workers created by this factory.
    factory.start();
    System.out.println("Activity Worker started for task queue: " + TASK_QUEUE);
  }
}
