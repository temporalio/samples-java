

package io.temporal.samples.springboot.update;

import io.temporal.samples.springboot.update.model.Purchase;
import io.temporal.workflow.*;

@WorkflowInterface
public interface PurchaseWorkflow {
  @WorkflowMethod
  void start();

  @UpdateMethod
  boolean makePurchase(Purchase purchase);

  @UpdateValidatorMethod(updateName = "makePurchase")
  void makePurchaseValidator(Purchase purchase);

  @SignalMethod
  void exit();
}
