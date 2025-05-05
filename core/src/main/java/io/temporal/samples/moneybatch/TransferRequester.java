

package io.temporal.samples.moneybatch;

import io.temporal.client.BatchRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.Random;
import java.util.UUID;

public class TransferRequester {

  /** Number of withdrawals to batch */
  public static final int BATCH_SIZE = 3;

  @SuppressWarnings("CatchAndPrintStackTrace")
  public static void main(String[] args) {
    String reference = UUID.randomUUID().toString();
    int amountCents = (new Random().nextInt(5) + 1) * 25;
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient workflowClient = WorkflowClient.newInstance(service);

    String from = "account1";
    String to = "account2";
    WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setTaskQueue(AccountActivityWorker.TASK_QUEUE)
            .setWorkflowId(to)
            .build();
    AccountTransferWorkflow transferWorkflow =
        workflowClient.newWorkflowStub(AccountTransferWorkflow.class, options);
    // Signal with start sends a signal to a workflow starting it if not yet running
    BatchRequest request = workflowClient.newSignalWithStartRequest();
    request.add(transferWorkflow::deposit, to, BATCH_SIZE);
    request.add(transferWorkflow::withdraw, from, reference, amountCents);
    workflowClient.signalWithStart(request);

    System.out.printf("Transfer of %d cents from %s to %s requested", amountCents, from, to);
    System.exit(0);
  }
}
