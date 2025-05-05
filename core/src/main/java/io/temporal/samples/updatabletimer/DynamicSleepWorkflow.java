

package io.temporal.samples.updatabletimer;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DynamicSleepWorkflow {
  @WorkflowMethod
  void execute(long wakeUpTime);

  @SignalMethod
  void updateWakeUpTime(long wakeUpTime);

  @QueryMethod
  long getWakeUpTime();
}
