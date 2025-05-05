

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
