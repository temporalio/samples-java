package io.temporal.samples.batch.heartbeatingactivity;

import static io.temporal.samples.batch.heartbeatingactivity.HeartbeatingActivityBatchWorker.TASK_QUEUE;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;

/** Starts a single execution of HeartbeatingActivityBatchWorkflow. */
public class HeartbeatingActivityBatchStarter {

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
    WorkflowClient workflowClient =
        WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
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
