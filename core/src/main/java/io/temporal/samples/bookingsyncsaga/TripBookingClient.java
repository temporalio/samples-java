package io.temporal.samples.bookingsyncsaga;

import com.google.common.base.Throwables;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;

public class TripBookingClient {

  static final String TASK_QUEUE = "TripBookingSync";

  public static void main(String[] args) {
    // Load configuration from environment and files
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    // gRPC stubs wrapper that talks to the temporal service.
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

    WorkflowOptions options =
        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId("Booking1").build();
    TripBookingWorkflow trip1 = client.newWorkflowStub(TripBookingWorkflow.class, options);
    // Start workflow asynchronously
    WorkflowClient.start(trip1::bookTrip, "trip1");
    try {
      // Wait for workflow to complete or fail the booking using an update.
      Booking booking = trip1.waitForBooking();
      System.out.println("Booking: " + booking);
    } catch (Exception e) {
      System.out.println(Throwables.getStackTraceAsString(e));
    }
    System.exit(0);
  }
}
