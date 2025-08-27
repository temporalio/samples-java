package io.temporal.samples.earlyreturn;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionWorkflowImpl implements TransactionWorkflow {
  private static final Logger log = LoggerFactory.getLogger(TransactionWorkflowImpl.class);
  private final TransactionActivities activities =
      Workflow.newActivityStub(
          TransactionActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30)).build());

  private boolean initDone = false;
  private Transaction tx;
  private Exception initError = null;

  @Override
  public TxResult processTransaction(TransactionRequest txRequest) {
    this.tx = activities.mintTransactionId(txRequest);

    try {
      this.tx = activities.initTransaction(this.tx);
    } catch (Exception e) {
      initError = e;
    } finally {
      initDone = true; // Will unblock the early-return returnInitResult method
    }

    if (initError != null) {
      // If initialization failed, cancel the transaction
      activities.cancelTransaction(this.tx);
      return new TxResult("", "Transaction cancelled.");
    } else {
      activities.completeTransaction(this.tx);
      return new TxResult(this.tx.getId(), "Transaction completed successfully.");
    }
  }

  @Override
  public TxResult returnInitResult() {
    Workflow.await(() -> initDone); // Wait for the initialization step of the workflow to complete

    if (initError != null) {
      log.info("Initialization failed.");
      throw Workflow.wrap(initError);
    }

    return new TxResult(
        tx.getId(), "Initialization successful"); // Return the update result to the caller
  }
}
