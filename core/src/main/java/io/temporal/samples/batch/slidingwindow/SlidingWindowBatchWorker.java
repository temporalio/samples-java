package io.temporal.samples.batch.slidingwindow;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/** Hosts sliding window batch sample workflow and activity implementations. */
public final class SlidingWindowBatchWorker {

  static final String TASK_QUEUE = "SlidingWindow";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

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
