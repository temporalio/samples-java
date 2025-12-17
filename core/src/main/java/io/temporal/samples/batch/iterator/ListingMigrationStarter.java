package io.temporal.samples.batch.iterator;

import static io.temporal.samples.batch.iterator.IteratorBatchWorker.TASK_QUEUE;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class ListingMigrationStarter {

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient workflowClient = WorkflowClient.newInstance(service);
    WorkflowOptions options = WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build();
    ListingMigrationWorkflow migrationWorkflow =
        workflowClient.newWorkflowStub(ListingMigrationWorkflow.class, options);
    migrationWorkflow.execute();
    System.exit(0);
  }
}
