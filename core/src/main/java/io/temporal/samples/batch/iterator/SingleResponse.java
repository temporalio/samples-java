package io.temporal.samples.batch.iterator;

public class SingleResponse {

  private int input;
  private int result;

  public SingleResponse() {}

  public SingleResponse(int input, int result) {
    this.input = input;
    this.result = result;
  }

  public int getInput() {
    return input;
  }

  public int getResult() {
    return result;
  }

  @Override
  public String toString() {
    return "SingleResponse{" + "input=" + input + ", result=" + result + '}';
  }
}
