package io.temporal.samples.reproduce;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ReminderWorkflow {

  @WorkflowMethod
  void start();

  @SignalMethod
  void scheduleReminder(ScheduleReminderSignal signal);
}
