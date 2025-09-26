package io.temporal.samples.batch.slidingwindow;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;

/** Hosts sliding window batch sample workflow and activity implementations. */
public final class SlidingWindowBatchWorker {

  static final String TASK_QUEUE = "SlidingWindow";

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
    Worker worker = factory.newWorker(TASK_QUEUE);

    worker.registerWorkflowImplementationTypes(
        BatchWorkflowImpl.class,
        SlidingWindowBatchWorkflowImpl.class,
        RecordProcessorWorkflowImpl.class);
    worker.registerActivitiesImplementations(new RecordLoaderImpl());

    factory.start();

    System.out.println("Worker started for task queue: " + TASK_QUEUE);
  }
}
