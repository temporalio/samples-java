package io.temporal.samples.packetdelivery;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PacketDeliveryWorkflow {
  @WorkflowMethod
  String execute();

  @SignalMethod
  void confirmDelivery(int deliveryId);
}
