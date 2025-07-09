package io.temporal.samples.packetdelivery;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.CompletablePromise;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;

public class PacketDelivery {
  private Packet packet;
  private boolean deliveryConfirmation = false;
  private CompletablePromise delivered = Workflow.newPromise();
  private String deliveryConfirmationCode = "";

  private Logger logger = Workflow.getLogger(this.getClass().getName());

  private final PacketDeliveryActivities activities =
      Workflow.newActivityStub(
          PacketDeliveryActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(3)).build());

  public PacketDelivery(Packet packet) {
    this.packet = packet;
    processDeliveryAsync();
  }

  public Promise<Void> getDelivered() {
    return delivered;
  }

  public void processDeliveryAsync() {
    delivered.completeFrom(Async.procedure(this::processDelivery));
  }

  public void processDelivery() {
    while (!deliveryConfirmationCode.equals("Confirmed")) {
      // Step 1 perform delivery
      logger.info(
          "** Performing delivery for packet: " + packet.getId() + " - " + packet.getContent());
      activities.performDelivery(packet);
      // Step 2 wait for delivery confirmation
      logger.info(
          "** Delivery for packet: "
              + packet.getId()
              + " - "
              + packet.getContent()
              + " awaiting delivery confirmation");
      Workflow.await(() -> deliveryConfirmation);
      logger.info(
          "** Delivery for packet: "
              + packet.getId()
              + " - "
              + packet.getContent()
              + " received confirmation");
      // Step 3 complete delivery processing
      logger.info(
          "** Completing delivery for packet: " + packet.getId() + " - " + packet.getContent());
      deliveryConfirmationCode = activities.completeDelivery(packet);
      // Reset deliveryConfirmation
      deliveryConfirmation = false;
    }
  }

  public void confirmDelivery() {
    this.deliveryConfirmation = true;
  }
}
