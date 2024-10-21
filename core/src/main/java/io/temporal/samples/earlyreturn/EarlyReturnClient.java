package io.temporal.samples.earlyreturn;

import io.temporal.client.*;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class EarlyReturnClient {
  private static final String TASK_QUEUE = "EarlyReturnTaskQueue";
  private static final String WORKFLOW_ID_PREFIX = "early-return-workflow-";

  public static void main(String[] args) {
    WorkflowClient client = setupWorkflowClient();
    runWorkflowWithUpdateWithStart(client);
  }

  // Setup the WorkflowClient
  public static WorkflowClient setupWorkflowClient() {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    return WorkflowClient.newInstance(service);
  }

  // Run workflow using 'updateWithStart'
  private static void runWorkflowWithUpdateWithStart(WorkflowClient client) {
    Transaction tx = new Transaction("", "Bob", "Alice", -1000);

    WorkflowOptions options = buildWorkflowOptions();
    TransactionWorkflow workflow = client.newWorkflowStub(TransactionWorkflow.class, options);

    try {
      System.out.println("Starting workflow with UpdateWithStart");

      UpdateWithStartWorkflowOperation<String> updateOp =
              UpdateWithStartWorkflowOperation.newBuilder(workflow::returnInitResult)
                      .setWaitForStage(WorkflowUpdateStage.COMPLETED) // Wait for update to complete
                      .build();

      WorkflowUpdateHandle<String> updateHandle =
              WorkflowClient.updateWithStart(workflow::processTransaction, tx, updateOp);

      String transactionId = updateHandle.getResultAsync().get();

      System.out.println("Transaction initialized successfully: " + transactionId);

      // TODO get the result of the workflow

    } catch (Exception e) {
      System.err.println("Transaction initialization failed: " + e.getMessage());
    }
  }

  // Build WorkflowOptions with task queue and unique ID
  private static WorkflowOptions buildWorkflowOptions() {
    return WorkflowOptions.newBuilder()
        .setTaskQueue(TASK_QUEUE)
        .setWorkflowId(WORKFLOW_ID_PREFIX + System.currentTimeMillis())
        .build();
  }
}
