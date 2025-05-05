package io.temporal.samples.springboot.update;

import io.temporal.activity.ActivityInterface;
import io.temporal.samples.springboot.update.model.Purchase;

@ActivityInterface
public interface PurchaseActivities {
  boolean isProductInStockForPurchase(Purchase purchase);

  boolean makePurchase(Purchase purchase);
}
