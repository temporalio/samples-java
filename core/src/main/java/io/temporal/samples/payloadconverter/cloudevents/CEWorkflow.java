package io.temporal.samples.payloadconverter.cloudevents;

import io.cloudevents.CloudEvent;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CEWorkflow {
  @WorkflowMethod
  void exec(CloudEvent cloudEvent);

  @SignalMethod
  void addEvent(CloudEvent cloudEvent);

  @QueryMethod
  CloudEvent getLastEvent();
}
