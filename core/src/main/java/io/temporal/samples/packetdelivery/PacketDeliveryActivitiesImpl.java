package io.temporal.samples.packetdelivery;

import java.util.ArrayList;
import java.util.List;

public class PacketDeliveryActivitiesImpl implements PacketDeliveryActivities {
  @Override
  public List<Packet> generatePackets() {
    List<Packet> result = new ArrayList<>();
    result.add(new Packet(1, "books"));
    result.add(new Packet(2, "jewelry"));
    result.add(new Packet(3, "furniture"));
    result.add(new Packet(4, "food"));
    result.add(new Packet(5, "electronics"));
    return result;
  }

  @Override
  public void performDelivery(Packet packet) {
    System.out.println(
        "** Activity - Performing delivery for packet: "
            + packet.getId()
            + " with content: "
            + packet.getContent());
    sleep(2);
  }

  @Override
  public String completeDelivery(Packet packet) {
    System.out.println(
        "** Activity - Completing delivery for package: "
            + packet.getId()
            + " with content: "
            + packet.getContent());
    sleep(1);
    // for sample we just confirm
    return "Confirmed";
  }

  private void sleep(int seconds) {
    try {
      Thread.sleep(seconds * 1000L);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
