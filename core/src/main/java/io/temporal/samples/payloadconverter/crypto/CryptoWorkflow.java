

package io.temporal.samples.payloadconverter.crypto;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CryptoWorkflow {
  @WorkflowMethod
  MyCustomer exec(MyCustomer customer);
}
