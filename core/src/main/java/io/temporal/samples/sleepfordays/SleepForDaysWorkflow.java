package io.temporal.samples.sleepfordays;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface SleepForDaysWorkflow {
  @WorkflowMethod
  String sleepForDays();

  @SignalMethod
  void complete();
}
