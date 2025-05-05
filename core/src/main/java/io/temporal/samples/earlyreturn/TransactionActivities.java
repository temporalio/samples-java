package io.temporal.samples.earlyreturn;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TransactionActivities {
  @ActivityMethod
  Transaction mintTransactionId(TransactionRequest txRequest);

  @ActivityMethod
  Transaction initTransaction(Transaction tx);

  @ActivityMethod
  void cancelTransaction(Transaction tx);

  @ActivityMethod
  void completeTransaction(Transaction tx);
}
