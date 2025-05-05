

package io.temporal.samples.batch.iterator;

import static io.temporal.samples.batch.iterator.IteratorBatchWorker.TASK_QUEUE;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

/** Starts a single execution of IteratorBatchWorkflow. */
public class IteratorBatchStarter {

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient workflowClient = WorkflowClient.newInstance(service);
    WorkflowOptions options = WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build();
    IteratorBatchWorkflow batchWorkflow =
        workflowClient.newWorkflowStub(IteratorBatchWorkflow.class, options);
    WorkflowExecution execution = WorkflowClient.start(batchWorkflow::processBatch, 5, 0);
    System.out.println(
        "Started batch workflow. WorkflowId="
            + execution.getWorkflowId()
            + ", RunId="
            + execution.getRunId());
    System.exit(0);
  }
}
