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
  public String processTransaction(Transaction txInput) {
    this.tx = txInput;
    try {
      this.tx = activities.initTransaction(this.tx);
    } catch (Exception e) {
      initError = e;
    } finally {
      initDone = true;
    }

    if (initError != null) {
      activities.cancelTransaction(this.tx);
      return "Transaction cancelled";
    } else {
      activities.completeTransaction(this.tx);
      return "Transaction completed successfully: " + this.tx.getId();
    }
  }

  @Override
  public String returnInitResult() {
    Workflow.await(() -> initDone);
    if (initError != null) {
      log.info("Initialization failed.");
      throw Workflow.wrap(initError);
    }
    return tx.getId();
  }
}
