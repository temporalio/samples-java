package io.temporal.samples.complex;

public class ComplexWorkflowImpl implements ComplexWorkflow {
  @Override
  public void handleLambda(Input input) {
    System.out.println(input.apply());
  }
}
