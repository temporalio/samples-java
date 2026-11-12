package io.temporal.samples.externalstorage;

import static io.temporal.samples.externalstorage.ExternalStorageWorker.TASK_QUEUE;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;

public class Starter {

  static final String WORKFLOW_ID = "external-storage-workflow-id";

  public static void main(String[] args) throws IOException {
    ClientConfigProfile profile = ClientConfigProfile.load();
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    WorkflowClient client =
        WorkflowClient.newInstance(
            service, StorageConfig.withExternalStorage(profile.toWorkflowClientOptions()));

    ProcessOrderBatchWorkflow workflow =
        client.newWorkflowStub(
            ProcessOrderBatchWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    WorkflowExecution execution = WorkflowClient.start(workflow::run, 200);
    System.out.println("Started workflow, run ID: " + execution.getRunId());

    BatchSummary summary = WorkflowStub.fromTyped(workflow).getResult(BatchSummary.class);
    System.out.printf(
        "Processed %d orders weighing %.1f kg in total%n",
        summary.orderCount(), summary.totalWeightKg());
    System.exit(0);
  }
}
