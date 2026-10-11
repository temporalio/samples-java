package io.temporal.samples.moneybatch;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AccountTransferWorkflowImpl implements AccountTransferWorkflow {

  private final ActivityOptions options =
      ActivityOptions.newBuilder()
          .setStartToCloseTimeout(Duration.ofSeconds(5))
          .setRetryOptions(
              RetryOptions.newBuilder()
                  .setInitialInterval(Duration.ofSeconds(1))
                  .setMaximumInterval(Duration.ofSeconds(10))
                  .build())
          .build();

  private final Account account = Workflow.newActivityStub(Account.class, options);

  private final Set<String> references;
  private final Deque<BatchState.Withdrawal> pending;
  private int balance;
  private int count;

  @WorkflowInit
  public AccountTransferWorkflowImpl(String toAccount, int batchSize, BatchState state) {
    // Signals can run before deposit(), including immediately after Continue-As-New.
    BatchState initial = state == null ? new BatchState() : state;
    references = new HashSet<>(initial.references);
    pending = new ArrayDeque<>(initial.pending);
  }

  @Override
  public void deposit(String toAccount, int batchSize, BatchState state) {
    if (batchSize <= 0) {
      throw ApplicationFailure.newNonRetryableFailure(
          "batchSize must be positive", "InvalidBatchSize");
    }
    Workflow.await(() -> pending.size() >= batchSize);
    // Select the whole batch before yielding to any Activity. Later signals only enter pending.
    List<BatchState.Withdrawal> batch = new ArrayList<>();
    for (int i = 0; i < batchSize; i++) {
      batch.add(pending.removeFirst());
    }
    List<Promise<Void>> withdrawals = new ArrayList<>();
    for (BatchState.Withdrawal withdrawal : batch) {
      withdrawals.add(
          Async.procedure(
              () -> {
                account.withdraw(
                    withdrawal.fromAccountId, withdrawal.referenceId, withdrawal.amountCents);
                balance += withdrawal.amountCents;
                count++;
              }));
    }
    Promise<Void> completed = Promise.allOf(withdrawals);
    // Only the selected N requests can increment count. Also wake on an Activity failure.
    Workflow.await(() -> count == batchSize || completed.isCompleted());
    completed.get();
    String referenceId = Workflow.randomUUID().toString();
    account.deposit(toAccount, referenceId, balance);

    if (!pending.isEmpty()) {
      Workflow.continueAsNew(toAccount, batchSize, new BatchState(pending, references));
    }
  }

  @Override
  public void withdraw(String fromAccountId, String referenceId, int amountCents) {
    if (!references.add(referenceId)) {
      return; // duplicate
    }
    pending.addLast(new BatchState.Withdrawal(fromAccountId, referenceId, amountCents));
  }

  @Override
  public int getBalance() {
    return balance;
  }

  @Override
  public int getCount() {
    return count;
  }
}
