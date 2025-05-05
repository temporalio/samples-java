

package io.temporal.samples.moneybatch;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface AccountTransferWorkflow {

  @WorkflowMethod
  void deposit(String toAccountId, int batchSize);

  @SignalMethod
  void withdraw(String fromAccountId, String referenceId, int amountCents);

  @QueryMethod
  int getBalance();

  @QueryMethod
  int getCount();
}
