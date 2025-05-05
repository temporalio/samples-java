

package io.temporal.samples.earlyreturn;

import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface TransactionWorkflow {
  @WorkflowMethod
  TxResult processTransaction(TransactionRequest txRequest);

  @UpdateMethod(name = "early-return")
  TxResult returnInitResult();
}
