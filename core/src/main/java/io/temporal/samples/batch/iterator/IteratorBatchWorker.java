

package io.temporal.samples.batch.iterator;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * A worker process that hosts implementations of IteratorBatchWorkflow and RecordProcessorWorkflow
 * as well as RecordLoader activity.
 */
public final class IteratorBatchWorker {

  static final String TASK_QUEUE = "IteratorBatch";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);

    worker.registerWorkflowImplementationTypes(
        IteratorBatchWorkflowImpl.class, RecordProcessorWorkflowImpl.class);

    worker.registerActivitiesImplementations(new RecordLoaderImpl());
    factory.start();
    System.out.println("Worker started for task queue: " + TASK_QUEUE);
  }
}
