package io.temporal.samples.retryonsignalinterceptor;

import io.temporal.activity.Activity;
import io.temporal.failure.ApplicationFailure;

public class MyActivityImpl implements MyActivity {
  @Override
  public void execute() {
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (Activity.getExecutionContext().getInfo().getAttempt() < 5) {
      throw ApplicationFailure.newFailure("simulated", "type1");
    }
  }
}
