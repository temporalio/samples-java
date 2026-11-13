package io.temporal.samples.googleadk.longrunning;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface LongRunningWorkflow {
  @WorkflowMethod
  String prepare(String request);

  @QueryMethod
  String pendingCall();

  @SignalMethod
  void complete(String functionCallId, String result);
}
