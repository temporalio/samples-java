

package io.temporal.samples.batch.slidingwindow;

import static io.temporal.samples.batch.slidingwindow.SlidingWindowBatchWorker.TASK_QUEUE;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class SlidingWindowBatchStarter {

  @SuppressWarnings("CatchAndPrintStackTrace")
  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient workflowClient = WorkflowClient.newInstance(service);
    WorkflowOptions options = WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build();
    BatchWorkflow batchWorkflow = workflowClient.newWorkflowStub(BatchWorkflow.class, options);
    WorkflowClient.start(batchWorkflow::processBatch, 10, 25, 3);
    System.out.println("Started batch workflow with 3 partitions");
    System.exit(0);
  }
}
