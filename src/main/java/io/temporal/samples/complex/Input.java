package io.temporal.samples.complex;

public class Input {
  private int i;

  public Input() {
    this.i = 0;
  }

  public Input(int i) {
    this.i = i;
  }

  public String apply() {
    return "name: " + i;
  }
}
