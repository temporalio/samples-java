

package io.temporal.samples.encodefailures;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CustomerAgeCheck {
  @WorkflowMethod
  public String validateCustomer(MyCustomer customer);
}
