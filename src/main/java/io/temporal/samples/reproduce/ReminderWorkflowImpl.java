package io.temporal.samples.reproduce;

import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowQueue;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;

public final class ReminderWorkflowImpl implements ReminderWorkflow {

  private static final Logger logger = Workflow.getLogger(ReminderWorkflowImpl.class);

  private final WorkflowQueue<ScheduleReminderSignal> signalQueue = Workflow.newQueue(10);

  private Promise<Void> activeReminder;
  private CancellationScope activeScope;

  @Override
  public void start() {
    ScheduleReminderSignal signalToProcess = signalQueue.take();
    while (signalToProcess != null) {
      processScheduleReminder(signalToProcess);

      Workflow.await(
          () ->
              signalQueue.peek() != null || activeReminder == null || activeReminder.isCompleted());

      signalToProcess = signalQueue.poll();
    }
  }

  @Override
  public void scheduleReminder(ScheduleReminderSignal signal) {
    logger.info("Got signal {}", signal);
    signalQueue.put(signal);
  }

  private void processScheduleReminder(ScheduleReminderSignal signal) {
    if (activeScope != null) {
      logger.info("Cancelling previous reminder");
      activeScope.cancel("New reminder");
      if (activeReminder != null) {
        try {
          // Consume the cancelled promise to avoid noisy warnings
          activeReminder.get();
        } catch (Exception ignored) {
        }
      }
    }

    activeScope =
        Workflow.newCancellationScope(
            () -> {
              Instant now = Instant.ofEpochMilli(Workflow.currentTimeMillis());
              Duration reminderSleepDuration = Duration.between(now, signal.getReminderTime());
              if (reminderSleepDuration.isNegative()) {
                logger.info("Got a request {} for an outdated reminder, ignoring it", signal);
                activeReminder = null;
                return;
              }
              logger.info("Scheduled a reminder for time {}", signal.getReminderTime());
              activeReminder =
                  Workflow.newTimer(reminderSleepDuration)
                      .thenApply(
                          (t) -> {
                            logger.info("Reminder: {}", signal.getReminderText());
                            return null;
                          });
            });
    activeScope.run();
  }
}
