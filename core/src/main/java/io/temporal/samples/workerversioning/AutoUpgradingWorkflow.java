package io.temporal.samples.workerversioning;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface AutoUpgradingWorkflow {

  @WorkflowMethod
  void run();

  @SignalMethod
  void doNextSignal(String signal);
}
