package io.temporal.samples.bookingsaga;

import com.google.common.base.Throwables;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class TripBookingClient {

  static final String TASK_QUEUE = "TripBooking";

  public static void main(String[] args) {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkflowOptions options =
        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId("Booking1").build();
    TripBookingWorkflow trip = client.newWorkflowStub(TripBookingWorkflow.class, options);
    try {
      Booking booking = trip.bookTrip("trip1");
      System.out.println("Booking: " + booking);
    } catch (Exception e) {
      System.out.println(Throwables.getStackTraceAsString(e));
    }
    System.exit(0);
  }
}
