

package io.temporal.samples.updatabletimer;

import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;

public final class UpdatableTimer {

  private final Logger logger = Workflow.getLogger(UpdatableTimer.class);

  private long wakeUpTime;
  private boolean wakeUpTimeUpdated;

  public void sleepUntil(long wakeUpTime) {
    logger.info("sleepUntil: " + Instant.ofEpochMilli(wakeUpTime));
    this.wakeUpTime = wakeUpTime;
    while (true) {
      wakeUpTimeUpdated = false;
      Duration sleepInterval = Duration.ofMillis(this.wakeUpTime - Workflow.currentTimeMillis());
      logger.info("Going to sleep for " + sleepInterval);
      if (!Workflow.await(sleepInterval, () -> wakeUpTimeUpdated)) {
        break;
      }
    }
    logger.info("sleepUntil completed");
  }

  public void updateWakeUpTime(long wakeUpTime) {
    logger.info("updateWakeUpTime: " + Instant.ofEpochMilli(wakeUpTime));
    this.wakeUpTime = wakeUpTime;
    this.wakeUpTimeUpdated = true;
  }

  public long getWakeUpTime() {
    return wakeUpTime;
  }
}
