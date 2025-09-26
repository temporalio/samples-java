package io.temporal.samples.moneybatch;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

public class AccountTransferWorker {

  @SuppressWarnings("CatchAndPrintStackTrace")
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

    Worker worker = factory.newWorker(AccountActivityWorker.TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(AccountTransferWorkflowImpl.class);

    factory.start();
    System.out.println("Worker started for task queue: " + AccountActivityWorker.TASK_QUEUE);
  }
}
