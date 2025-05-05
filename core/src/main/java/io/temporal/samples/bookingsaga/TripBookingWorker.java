package io.temporal.samples.bookingsaga;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class TripBookingWorker {

  @SuppressWarnings("CatchAndPrintStackTrace")
  public static void main(String[] args) {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);

    // Worker that listens on a task queue and hosts both workflow and activity implementations.
    Worker worker = factory.newWorker(TripBookingClient.TASK_QUEUE);

    // Workflows are stateful. So you need a type to create instances.
    worker.registerWorkflowImplementationTypes(TripBookingWorkflowImpl.class);

    // Activities are stateless and thread safe. So a shared instance is used.
    TripBookingActivities tripBookingActivities = new TripBookingActivitiesImpl();
    worker.registerActivitiesImplementations(tripBookingActivities);

    // Start all workers created by this factory.
    factory.start();
    System.out.println("Worker started for task queue: " + TripBookingClient.TASK_QUEUE);
  }
}
