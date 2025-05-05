

package io.temporal.samples.moneytransfer;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class AccountTransferWorkflowImpl implements AccountTransferWorkflow {

  private final ActivityOptions options =
      ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build();
  private final Account account = Workflow.newActivityStub(Account.class, options);

  @Override
  public void transfer(
      String fromAccountId, String toAccountId, String referenceId, int amountCents) {
    account.withdraw(fromAccountId, referenceId, amountCents);
    account.deposit(toAccountId, referenceId, amountCents);
  }
}
