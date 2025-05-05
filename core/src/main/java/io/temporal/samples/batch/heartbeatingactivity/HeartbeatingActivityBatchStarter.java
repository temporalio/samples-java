

package io.temporal.samples.batch.heartbeatingactivity;

import static io.temporal.samples.batch.heartbeatingactivity.HeartbeatingActivityBatchWorker.TASK_QUEUE;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

/** Starts a single execution of HeartbeatingActivityBatchWorkflow. */
public class HeartbeatingActivityBatchStarter {

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient workflowClient = WorkflowClient.newInstance(service);
    WorkflowOptions options = WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build();
    HeartbeatingActivityBatchWorkflow batchWorkflow =
        workflowClient.newWorkflowStub(HeartbeatingActivityBatchWorkflow.class, options);
    WorkflowExecution execution = WorkflowClient.start(batchWorkflow::processBatch);
    System.out.println(
        "Started batch workflow. WorkflowId="
            + execution.getWorkflowId()
            + ", RunId="
            + execution.getRunId());
    System.exit(0);
  }
}
