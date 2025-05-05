

package io.temporal.samples.encodefailures;

public class InvalidCustomerException extends Exception {
  public InvalidCustomerException(String errorMessage) {
    super(errorMessage);
  }
}
