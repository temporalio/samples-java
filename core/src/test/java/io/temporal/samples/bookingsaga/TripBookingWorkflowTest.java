package io.temporal.samples.bookingsaga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.temporal.client.WorkflowException;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;

public class TripBookingWorkflowTest {

  @RegisterExtension
  public static final TestWorkflowExtension testWorkflowExtension =
      TestWorkflowExtension.newBuilder()
          .setWorkflowTypes(TripBookingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  /**
   * Not very useful test that validates that the default activities cause workflow to fail. See
   * other tests on using mocked activities to test SAGA logic.
   */
  @Test
  public void testTripBookingFails(
      TestWorkflowEnvironment testEnv, Worker worker, TripBookingWorkflow workflow) {
    worker.registerActivitiesImplementations(new TripBookingActivitiesImpl());
    testEnv.start();

    WorkflowException exception =
        assertThrows(WorkflowException.class, () -> workflow.bookTrip("trip1"));
    assertEquals(
        "Flight booking did not work",
        ((ApplicationFailure) exception.getCause().getCause()).getOriginalMessage());
  }

  /** Unit test workflow logic using mocked activities. */
  @Test
  public void testSAGA(
      TestWorkflowEnvironment testEnv, Worker worker, TripBookingWorkflow workflow) {
    TripBookingActivities activities = mock(TripBookingActivities.class);

    ArgumentCaptor<String> captorHotelRequestId = ArgumentCaptor.forClass(String.class);
    when(activities.bookHotel(captorHotelRequestId.capture(), eq("trip1")))
        .thenReturn("HotelBookingID1");

    ArgumentCaptor<String> captorCarRequestId = ArgumentCaptor.forClass(String.class);
    when(activities.reserveCar(captorCarRequestId.capture(), eq("trip1")))
        .thenReturn("CarBookingID1");

    ArgumentCaptor<String> captorFlightRequestId = ArgumentCaptor.forClass(String.class);
    when(activities.bookFlight(captorFlightRequestId.capture(), eq("trip1")))
        .thenThrow(
            ApplicationFailure.newNonRetryableFailure(
                "Flight booking did not work", "bookingFailure"));
    worker.registerActivitiesImplementations(activities);
    testEnv.start();

    WorkflowException exception =
        assertThrows(WorkflowException.class, () -> workflow.bookTrip("trip1"));
    assertEquals(
        "Flight booking did not work",
        ((ApplicationFailure) exception.getCause().getCause()).getOriginalMessage());

    verify(activities).cancelHotel(captorHotelRequestId.getValue(), "trip1");
    verify(activities).cancelCar(captorCarRequestId.getValue(), "trip1");
    verify(activities).cancelFlight(captorFlightRequestId.getValue(), "trip1");
  }
}
