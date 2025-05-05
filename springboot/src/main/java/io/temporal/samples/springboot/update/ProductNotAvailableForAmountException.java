package io.temporal.samples.springboot.update;

public class ProductNotAvailableForAmountException extends Exception {
  public ProductNotAvailableForAmountException(String message) {
    super(message);
  }
}
