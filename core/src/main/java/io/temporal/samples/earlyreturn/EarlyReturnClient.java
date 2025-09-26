package io.temporal.samples.earlyreturn;

import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.client.*;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;

public class EarlyReturnClient {
  private static final String TASK_QUEUE = "EarlyReturnTaskQueue";
  private static final String WORKFLOW_ID_PREFIX = "early-return-workflow-";

  public static void main(String[] args) {
    WorkflowClient client = setupWorkflowClient();
    runWorkflowWithUpdateWithStart(client);
  }

  // Set up the WorkflowClient
  public static WorkflowClient setupWorkflowClient() {
    // Load configuration from environment and files
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    return WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
  }

  // Run workflow using 'updateWithStart'
  private static void runWorkflowWithUpdateWithStart(WorkflowClient client) {
    TransactionRequest txRequest =
        new TransactionRequest(
            "Bob", "Alice",
            1000); // Change this amount to a negative number to have initTransaction fail

    WorkflowOptions options = buildWorkflowOptions();
    TransactionWorkflow workflow = client.newWorkflowStub(TransactionWorkflow.class, options);

    System.out.println("Starting workflow with UpdateWithStart");

    TxResult updateResult = null;
    try {
      updateResult =
          WorkflowClient.executeUpdateWithStart(
              workflow::returnInitResult,
              UpdateOptions.<TxResult>newBuilder().build(),
              new WithStartWorkflowOperation<>(workflow::processTransaction, txRequest));

      System.out.println(
          "Workflow initialized with result: "
              + updateResult.getStatus()
              + " (transactionId: "
              + updateResult.getTransactionId()
              + ")");

      TxResult result = WorkflowStub.fromTyped(workflow).getResult(TxResult.class);
      System.out.println(
          "Workflow completed with result: "
              + result.getStatus()
              + " (transactionId: "
              + result.getTransactionId()
              + ")");
    } catch (Exception e) {
      System.err.println("Transaction initialization failed: " + e.getMessage());
    }
  }

  // Build WorkflowOptions with task queue and unique ID
  private static WorkflowOptions buildWorkflowOptions() {
    return WorkflowOptions.newBuilder()
        .setTaskQueue(TASK_QUEUE)
        .setWorkflowIdConflictPolicy(WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_FAIL)
        .setWorkflowId(WORKFLOW_ID_PREFIX + System.currentTimeMillis())
        .build();
  }
}
