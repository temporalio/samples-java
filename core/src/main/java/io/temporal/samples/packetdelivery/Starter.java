package io.temporal.samples.packetdelivery;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.util.Collections;
import java.util.List;

public class Starter {
  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker("packet-delivery-taskqueue");

    worker.registerWorkflowImplementationTypes(PacketDeliveryWorkflowImpl.class);
    worker.registerActivitiesImplementations(new PacketDeliveryActivitiesImpl(client));

    factory.start();

    PacketDeliveryWorkflow workflow =
        client.newWorkflowStub(
            PacketDeliveryWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("packet-delivery-workflow")
                .setTaskQueue("packet-delivery-taskqueue")
                .build());

    WorkflowClient.start(workflow::execute);

    // start completing package deliveries (send confirmations)
    // Query workflow for packets that need confirmation, confirm until none need confirmation any
    // more
    while (true) {
      sleep(3);
      // for "fun", reverse the list we get from delivery confirmation list
      List<Packet> packets = workflow.deliveryConfirmationPackets();
      if (packets.isEmpty()) {
        break;
      }
      // for "fun", reverse the list we get from delivery confirmation list
      Collections.reverse(packets);

      for (Packet p : packets) {
        try {
          workflow.confirmDelivery(p.getId());
        } catch (WorkflowNotFoundException e) {
          // In some cases with cancellations happening, workflow could be completed by now
          // We just ignore and exit out of loop
          break;
        }
      }
    }

    // wait for workflow to complete
    String result = WorkflowStub.fromTyped(workflow).getResult(String.class);
    System.out.println("** Workflow Result: " + result);
  }

  private static void sleep(int seconds) {
    try {
      Thread.sleep(seconds * 1000L);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
