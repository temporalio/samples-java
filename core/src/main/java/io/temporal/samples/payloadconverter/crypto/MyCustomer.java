

package io.temporal.samples.payloadconverter.crypto;

import com.codingrodent.jackson.crypto.Encrypt;

public class MyCustomer {
  private String name;
  private int age;
  private boolean approved;

  public MyCustomer() {}

  public MyCustomer(String name, int age) {
    this.name = name;
    this.age = age;
  }

  @Encrypt
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Encrypt
  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  @Encrypt
  public boolean isApproved() {
    return approved;
  }

  public void setApproved(boolean approved) {
    this.approved = approved;
  }
}
