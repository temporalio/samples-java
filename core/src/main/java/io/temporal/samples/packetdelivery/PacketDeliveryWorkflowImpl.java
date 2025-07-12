package io.temporal.samples.packetdelivery;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class PacketDeliveryWorkflowImpl implements PacketDeliveryWorkflow {
  private final Map<Integer, PacketDelivery> packetDeliveries = new HashMap<>();
  private final Logger logger = Workflow.getLogger(this.getClass().getName());

  private final PacketDeliveryActivities activities =
      Workflow.newActivityStub(
          PacketDeliveryActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(5))
              .setHeartbeatTimeout(Duration.ofSeconds(2))
              .build());

  @Override
  public String execute() {
    List<Promise<Void>> packetsDelivered = new ArrayList<>();
    // Step 1 - upload initial packets to deliver
    List<Packet> initialPackets = activities.generatePackets();
    // Step 2 - set up delivery processing
    for (Packet packet : initialPackets) {
      PacketDelivery delivery = new PacketDelivery(packet);
      packetDeliveries.put(packet.getId(), delivery);
      packetsDelivered.add(delivery.getDelivered());
    }

    // Wait for all packet deliveries to complete
    Promise.allOf(packetsDelivered).get();
    return "completed";
  }

  @Override
  public void confirmDelivery(int deliveryId) {
    if (packetDeliveries.containsKey(deliveryId)) {
      packetDeliveries.get(deliveryId).confirmDelivery();
    }
  }

  @Override
  public void cancelDelivery(int deliveryId, String reason) {
    if (packetDeliveries.containsKey(deliveryId)) {
      // Only makes sense to cancel if delivery is not done yet
      if (!packetDeliveries.get(deliveryId).getDelivered().isCompleted()) {
        logger.info("Sending cancellation for delivery : " + deliveryId + " and reason: " + reason);
        packetDeliveries.get(deliveryId).cancelDelivery(reason);
      }
      logger.info(
          "Bypassing sending cancellation for delivery : "
              + deliveryId
              + " and reason: "
              + reason
              + " because delivery already completed");
    }
  }

  @Override
  public List<Packet> deliveryConfirmationPackets() {
    List<Packet> confirmationPackets = new ArrayList<>();
    packetDeliveries
        .values()
        .forEach(
            p -> {
              if (p.isNeedDeliveryConfirmation()) {
                confirmationPackets.add(p.getPacket());
              }
            });
    return confirmationPackets;
  }
}
