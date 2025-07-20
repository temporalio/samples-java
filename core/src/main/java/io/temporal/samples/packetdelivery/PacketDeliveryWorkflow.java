package io.temporal.samples.packetdelivery;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;

@WorkflowInterface
public interface PacketDeliveryWorkflow {
  @WorkflowMethod
  String execute();

  @SignalMethod
  void confirmDelivery(int deliveryId);

  @SignalMethod
  void cancelDelivery(int deliveryId, String reason);

  @QueryMethod
  List<Packet> deliveryConfirmationPackets();
}
