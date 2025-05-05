package io.temporal.samples.springboot.update;

import io.temporal.activity.LocalActivityOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.springboot.update.model.Purchase;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import java.time.Duration;

@WorkflowImpl(taskQueues = "UpdateSampleTaskQueue")
public class PurchaseWorkflowImpl implements PurchaseWorkflow {

  private boolean newPurchase = false;
  private boolean exit = false;
  private PurchaseActivities activities =
      Workflow.newLocalActivityStub(
          PurchaseActivities.class,
          LocalActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

  @Override
  public void start() {
    // for sake of sample we only wait for a single purchase or exit signal
    Workflow.await(() -> newPurchase || exit);
  }

  @Override
  public boolean makePurchase(Purchase purchase) {

    if (!activities.isProductInStockForPurchase(purchase)) {
      throw ApplicationFailure.newFailure(
          "Product "
              + purchase.getProduct()
              + " is not in stock for amount "
              + purchase.getAmount(),
          ProductNotAvailableForAmountException.class.getName(),
          purchase);
    }

    return activities.makePurchase(purchase);
  }

  @Override
  public void makePurchaseValidator(Purchase purchase) {
    // Not allowed to change workflow state inside validator
    // So invocations of (local) activities is prohibited
    // We can validate the purchase with some business logic here

    // Assume we have some max inventory amount for single item set to 100
    if (purchase == null || (purchase.getAmount() < 0 || purchase.getAmount() > 100)) {
      throw new IllegalArgumentException(
          "Invalid Product or amount (Product id:"
              + purchase.getProduct()
              + ", amount"
              + purchase.getAmount()
              + ")");
    }
  }

  @Override
  public void exit() {
    this.exit = true;
  }
}
