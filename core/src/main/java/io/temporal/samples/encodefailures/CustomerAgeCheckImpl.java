package io.temporal.samples.encodefailures;

import io.temporal.workflow.Workflow;

public class CustomerAgeCheckImpl implements CustomerAgeCheck {
  @Override
  public String validateCustomer(MyCustomer customer) {
    // Note we have explicitly set InvalidCustomerException type to fail workflow execution
    // We wrap it using Workflow.wrap so can throw as unchecked
    if (customer.getAge() < 21) {
      throw Workflow.wrap(
          new InvalidCustomerException("customer " + customer.getName() + " is under age."));
    } else {
      return "done...";
    }
  }
}
