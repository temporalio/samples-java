

package io.temporal.samples.springboot.update.model;

public class Purchase {
  int product;
  int amount;

  public Purchase() {}

  public Purchase(int product, int amount) {
    this.product = product;
    this.amount = amount;
  }

  public int getProduct() {
    return product;
  }

  public void setProduct(int product) {
    this.product = product;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }
}
