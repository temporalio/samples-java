

package io.temporal.samples.moneytransfer;

import static io.temporal.samples.moneytransfer.AccountActivityWorker.TASK_QUEUE;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.Random;
import java.util.UUID;

public class TransferRequester {

  @SuppressWarnings("CatchAndPrintStackTrace")
  public static void main(String[] args) {
    String reference;
    int amountCents;
    if (args.length == 0) {
      reference = UUID.randomUUID().toString();
      amountCents = new Random().nextInt(5000);
    } else {
      reference = args[0];
      amountCents = Integer.parseInt(args[1]);
    }
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    // client that can be used to start and signal workflows
    WorkflowClient workflowClient = WorkflowClient.newInstance(service);

    // now we can start running instances of the saga - its state will be persisted
    WorkflowOptions options = WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build();
    AccountTransferWorkflow transferWorkflow =
        workflowClient.newWorkflowStub(AccountTransferWorkflow.class, options);
    String from = "account1";
    String to = "account2";
    WorkflowClient.start(transferWorkflow::transfer, from, to, reference, amountCents);
    System.out.printf("Transfer of %d cents from %s to %s requested", amountCents, from, to);
    System.exit(0);
  }
}
