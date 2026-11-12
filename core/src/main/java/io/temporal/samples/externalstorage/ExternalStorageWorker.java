package io.temporal.samples.externalstorage;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

public class ExternalStorageWorker {

  static final String TASK_QUEUE = "external-storage";

  public static void main(String[] args) throws IOException {
    ClientConfigProfile profile = ClientConfigProfile.load();
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    // The worker uses its client's data converter and external storage, so this is all it takes to
    // offload the large activity inputs and results it sends.
    WorkflowClient client =
        WorkflowClient.newInstance(
            service, StorageConfig.withExternalStorage(profile.toWorkflowClientOptions()));

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(ProcessOrderBatchWorkflowImpl.class);
    worker.registerActivitiesImplementations(new OrderActivitiesImpl());

    factory.start();
    System.out.println("Worker started on task queue: " + TASK_QUEUE);
  }
}
