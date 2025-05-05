package io.temporal.samples.retryonsignalinterceptor;

import io.temporal.failure.ApplicationFailure;
import java.util.concurrent.atomic.AtomicInteger;

public class MyActivityImpl implements MyActivity {

  /**
   * WARNING! Never rely on such shared state in real applications. The activity variables are per
   * process and in almost all cases multiple worker processes are used.
   */
  private final AtomicInteger count = new AtomicInteger();

  /** Sleeps 5 seconds. Fails for 4 first invocations, and then completes. */
  @Override
  public void execute() {
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (count.incrementAndGet() < 5) {
      throw ApplicationFailure.newFailure("simulated", "type1");
    }
  }
}
