

package io.temporal.samples.payloadconverter.crypto;

public class CryptoWorkflowImpl implements CryptoWorkflow {
  @Override
  public MyCustomer exec(MyCustomer customer) {
    // if > 18 "approve" otherwise deny
    if (customer.getAge() > 18) {
      customer.setApproved(true);
    } else {
      customer.setApproved(false);
    }
    return customer;
  }
}
