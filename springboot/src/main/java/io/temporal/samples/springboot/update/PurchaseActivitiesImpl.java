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

import io.temporal.samples.springboot.update.model.Product;
import io.temporal.samples.springboot.update.model.ProductRepository;
import io.temporal.samples.springboot.update.model.Purchase;
import io.temporal.spring.boot.ActivityImpl;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = "UpdateSampleTaskQueue")
public class PurchaseActivitiesImpl implements PurchaseActivities {
  @Autowired ProductRepository productRepository;

  @Override
  public boolean isProductInStockForPurchase(Purchase purchase) {
    Product product = getProductFor(purchase);
    return product != null && product.getStock() >= purchase.getAmount();
  }

  @Override
  public boolean makePurchase(Purchase purchase) {
    Product product = getProductFor(purchase);
    if (product != null) {
      product.setStock(product.getStock() - purchase.getAmount());
      productRepository.save(product);
      return true;
    }
    return false;
  }

  private Product getProductFor(Purchase purchase) {
    Optional<Product> productOptional = productRepository.findById(purchase.getProduct());
    if (productOptional.isPresent()) {
      return productOptional.get();
    }
    return null;
  }
}
