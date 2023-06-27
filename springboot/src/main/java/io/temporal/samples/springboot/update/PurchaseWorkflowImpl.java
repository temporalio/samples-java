/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.springboot.update;

import io.temporal.activity.LocalActivityOptions;
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
    return activities.makePurchase(purchase);
  }

  @Override
  public void makePurchaseValidator(Purchase purchase) {
    if (!activities.isProductInStockForPurchase(purchase)) {
      throw new IllegalArgumentException(
          "Product "
              + purchase.getProduct()
              + " is not in stock for amount "
              + purchase.getAmount());
    }
  }

  @Override
  public void exit() {
    this.exit = true;
  }
}
