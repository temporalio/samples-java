package io.temporal.samples.packetdelivery;

import io.temporal.activity.ActivityInterface;
import java.util.List;

@ActivityInterface
public interface PacketDeliveryActivities {
  List<Packet> generatePackets();

  void performDelivery(Packet packet);

  String completeDelivery(Packet packet);

  String compensateDelivery(Packet packet);
}
