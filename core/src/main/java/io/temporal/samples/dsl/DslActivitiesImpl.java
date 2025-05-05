package io.temporal.samples.dsl;

import java.util.concurrent.TimeUnit;

public class DslActivitiesImpl implements DslActivities {
  @Override
  public String one() {
    sleep(1);
    return "Activity one done...";
  }

  @Override
  public String two() {
    sleep(1);
    return "Activity two done...";
  }

  @Override
  public String three() {
    sleep(1);
    return "Activity three done...";
  }

  @Override
  public String four() {
    sleep(1);
    return "Activity four done...";
  }

  private void sleep(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException ee) {
      // Empty
    }
  }
}
