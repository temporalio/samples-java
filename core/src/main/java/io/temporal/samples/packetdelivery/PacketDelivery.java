package io.temporal.samples.packetdelivery;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.workflow.*;
import java.time.Duration;
import org.slf4j.Logger;

public class PacketDelivery {
  private Packet packet;
  private boolean deliveryConfirmation = false;
  private CompletablePromise delivered = Workflow.newPromise();
  private CancellationScope cancellationScope;

  private Logger logger = Workflow.getLogger(this.getClass().getName());

  private final PacketDeliveryActivities activities =
      Workflow.newActivityStub(
          PacketDeliveryActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(5))
              .setHeartbeatTimeout(Duration.ofSeconds(2))
              .build());

  private final PacketDeliveryActivities compensationActivities =
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
    cancellationScope =
        Workflow.newCancellationScope(
            () -> {
              String deliveryConfirmationResult = "";
              while (!deliveryConfirmationResult.equals(PacketUtils.COMPLETION_SUCCESS)) {
                // Step 1 perform delivery
                logger.info(
                    "** Performing delivery for packet: "
                        + packet.getId()
                        + " - "
                        + packet.getContent());
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
                    "** Completing delivery for packet: "
                        + packet.getId()
                        + " - "
                        + packet.getContent());
                deliveryConfirmationResult = activities.completeDelivery(packet);
                // Reset deliveryConfirmation
                deliveryConfirmation = false;
              }
            });

    try {
      cancellationScope.run();
    } catch (Exception e) {
      System.out.println("*************** E1: " + e.getClass().getName());
      if (e instanceof ActivityFailure) {
        ActivityFailure activityFailure = (ActivityFailure) e;
        if (activityFailure.getCause() instanceof CanceledFailure) {
          // Run compensation activity and complete
          System.out.println("*************** E11: " + e.getClass().getName());
          compensationActivities.compensateDelivery(packet);
        }
      }
      // Just for show for example that cancel could come in while we are waiting on approval signal
      // too
      else if (e instanceof CanceledFailure) {
        // Run compensation activity and complete
        compensationActivities.compensateDelivery(packet);
      }
      return;
    }
  }

  public void confirmDelivery() {
    this.deliveryConfirmation = true;
  }

  public void cancelDelivery(String reason) {
    if (cancellationScope != null) {
      cancellationScope.cancel(reason);
    }
  }
}
