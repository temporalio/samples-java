package io.temporal.samples.packetdelivery;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

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
    sleep(3);
    workflow.confirmDelivery(3); // furniture
    sleep(1);
    workflow.confirmDelivery(5); // electronics
    sleep(1);
    workflow.confirmDelivery(1); // books
    sleep(1);
    workflow.confirmDelivery(2); // jewelry
    sleep(1);
    workflow.confirmDelivery(4); // food

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
