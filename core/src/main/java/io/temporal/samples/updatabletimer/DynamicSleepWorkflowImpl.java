package io.temporal.samples.updatabletimer;

public class DynamicSleepWorkflowImpl implements DynamicSleepWorkflow {

  private UpdatableTimer timer = new UpdatableTimer();

  @Override
  public void execute(long wakeUpTime) {
    timer.sleepUntil(wakeUpTime);
  }

  @Override
  public void updateWakeUpTime(long wakeUpTime) {
    timer.updateWakeUpTime(wakeUpTime);
  }

  @Override
  public long getWakeUpTime() {
    return timer.getWakeUpTime();
  }
}
