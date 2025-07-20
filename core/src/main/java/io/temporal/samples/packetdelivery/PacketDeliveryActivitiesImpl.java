package io.temporal.samples.packetdelivery;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.client.ActivityCompletionException;
import io.temporal.client.WorkflowClient;
import java.util.*;

public class PacketDeliveryActivitiesImpl implements PacketDeliveryActivities {
  private List<Packet> packets =
      Arrays.asList(
          new Packet(1, "books"),
          new Packet(2, "jewelry"),
          new Packet(3, "furniture"),
          new Packet(4, "food"),
          new Packet(5, "electronics"));
  private WorkflowClient client;

  public PacketDeliveryActivitiesImpl(WorkflowClient client) {
    this.client = client;
  }

  @Override
  public List<Packet> generatePackets() {
    return packets;
  }

  @Override
  public void performDelivery(Packet packet) {
    ActivityExecutionContext context = Activity.getExecutionContext();
    System.out.println(
        "** Activity - Performing delivery for packet: "
            + packet.getId()
            + " with content: "
            + packet.getContent());
    for (int i = 0; i < 4; i++) {
      try {
        // Perform the heartbeat. Used to notify the workflow that activity execution is alive
        context.heartbeat(i);
      } catch (ActivityCompletionException e) {
        System.out.println(
            "** Activity - Canceling delivery activity for packet: "
                + packet.getId()
                + " with content: "
                + packet.getContent());
        throw e;
      }
    }
  }

  @Override
  public String completeDelivery(Packet packet) {
    ActivityExecutionContext context = Activity.getExecutionContext();
    System.out.println(
        "** Activity - Completing delivery for package: "
            + packet.getId()
            + " with content: "
            + packet.getContent());
    for (int i = 0; i < 4; i++) {
      try {
        // Perform the heartbeat. Used to notify the workflow that activity execution is alive
        context.heartbeat(i);
      } catch (ActivityCompletionException e) {
        System.out.println(
            "** Activity - Canceling complete delivery activity for packet: "
                + packet.getId()
                + " with content: "
                + packet.getContent());
        throw e;
      }
    }
    // For sample we just confirm
    return randomCompletionDeliveryResult(packet);
  }

  @Override
  public String compensateDelivery(Packet packet) {
    System.out.println(
        "** Activity - Compensating delivery for package: "
            + packet.getId()
            + " with content: "
            + packet.getContent());
    sleep(1);
    return PacketUtils.COMPENSATION_COMPLETED;
  }

  /**
   * For this sample activity completion result can drive if 1. Delivery confirmation is completed,
   * in which case we complete delivery 2. Delivery confirmation is failed, in which case we run the
   * delivery again 3. Delivery confirmation is cancelled, in which case we want to cancel delivery
   * and perform "cleanup activity" Note that any delivery can cancel itself OR another delivery, so
   * for example Furniure delivery can cancel the Food delivery. For sample we have some specific
   * rules Which delivery can cancel which
   */
  private String randomCompletionDeliveryResult(Packet packet) {
    Random random = new Random();
    double randomValue = random.nextDouble();
    if (randomValue < 0.10) { // 10% chance for delivery completion to be canceled
      int toCancelDelivery = determineCancelRules(packet);
      System.out.println(
          "** Activity - Delivery completion result for package: "
              + packet.getId()
              + " with content: "
              + packet.getContent()
              + ": "
              + "Cancelling delivery: "
              + toCancelDelivery);

      // send cancellation signal for packet to be canceled
      PacketDeliveryWorkflow packetWorkflow =
          client.newWorkflowStub(
              PacketDeliveryWorkflow.class,
              Activity.getExecutionContext().getInfo().getWorkflowId());
      packetWorkflow.cancelDelivery(toCancelDelivery, "canceled from delivery " + packet.getId());

      return PacketUtils.COMPLETION_CANCELLED;
    }
    if (randomValue < 0.20) { // 20% chance for delivery completion to fail
      System.out.println(
          "** Activity - Delivery completion result for package: "
              + packet.getId()
              + " with content: "
              + packet.getContent()
              + ": "
              + "Failed");
      return PacketUtils.COMPLETION_FAILURE;
    }

    System.out.println(
        "** Activity - Delivery completion result for package: "
            + packet.getId()
            + " with content: "
            + packet.getContent()
            + ": "
            + "Successful");
    return PacketUtils.COMPLETION_SUCCESS;
  }

  private void sleep(int seconds) {
    try {
      Thread.sleep(seconds * 1000L);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  /**
   * Sample rules for canceling different deliveries We just rotate the list 1-5 (packet ids) by
   * packet id and return first result
   */
  private int determineCancelRules(Packet packet) {
    List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
    Collections.rotate(list, packet.getId());
    System.out.println(
        "** Activity - Package delivery : "
            + packet.getId()
            + " canceling package delivery: "
            + list.get(0));
    return list.get(0);
  }
}
