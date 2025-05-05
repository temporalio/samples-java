

package io.temporal.samples.batch.heartbeatingactivity;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * A worker process that hosts implementations of HeartbeatingActivityBatchWorkflow and
 * RecordProcessorActivity.
 */
public final class HeartbeatingActivityBatchWorker {

  static final String TASK_QUEUE = "HeartbeatingActivityBatch";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);

    worker.registerWorkflowImplementationTypes(HeartbeatingActivityBatchWorkflowImpl.class);

    worker.registerActivitiesImplementations(
        new RecordProcessorActivityImpl(new RecordLoaderImpl(), new RecordProcessorImpl()));
    factory.start();
    System.out.println("Worker started for task queue: " + TASK_QUEUE);
  }
}
