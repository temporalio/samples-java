package io.temporal.samples.updatewithstart;

public class StartWorkflowRequest {
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  private String value;
}
