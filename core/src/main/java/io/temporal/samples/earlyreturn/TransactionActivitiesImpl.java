package io.temporal.samples.earlyreturn;

import io.temporal.failure.ApplicationFailure;

public class TransactionActivitiesImpl implements TransactionActivities {

  @Override
  public Transaction initTransaction(Transaction tx) {
    System.out.println("Initializing transaction");
    sleep(500);
    if (tx.getAmount() <= 0) {
      System.out.println("Invalid amount: " + tx.getAmount());
      throw ApplicationFailure.newNonRetryableFailure(
          "Non-retryable Activity Failure: Invalid Amount", "InvalidAmount");
    }
    tx.setId("TXID" + String.format("%010d", (long) (Math.random() * 1_000_000_0000L)));
    sleep(500);
    return tx;
  }

  @Override
  public void cancelTransaction(Transaction tx) {
    System.out.println("Cancelling transaction");
    sleep(2000);
    System.out.println("Transaction cancelled");
  }

  @Override
  public void completeTransaction(Transaction tx) {
    System.out.println(
        "Sending $"
            + tx.getAmount()
            + " from "
            + tx.getSourceAccount()
            + " to "
            + tx.getTargetAccount());
    sleep(2000);
    System.out.println("Transaction completed successfully");
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
