

package io.temporal.samples.bookingsaga;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class TripBookingWorkflowImpl implements TripBookingWorkflow {

  private final ActivityOptions options =
      ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build();
  private final TripBookingActivities activities =
      Workflow.newActivityStub(TripBookingActivities.class, options);

  @Override
  public Booking bookTrip(String name) {
    // Configure SAGA to run compensation activities in parallel
    Saga.Options sagaOptions = new Saga.Options.Builder().setParallelCompensation(true).build();
    Saga saga = new Saga(sagaOptions);
    try {
      // addCompensation is added before the actual call to handle situations when the call failed
      // due to a timeout and its success is not clear.
      // The compensation code must handle situations when the actual function wasn't executed
      // gracefully.
      String carReservationRequestId = Workflow.randomUUID().toString();
      saga.addCompensation(activities::cancelCar, carReservationRequestId, name);
      String carReservationID = activities.reserveCar(carReservationRequestId, name);

      String hotelReservationRequestID = Workflow.randomUUID().toString();
      saga.addCompensation(activities::cancelHotel, hotelReservationRequestID, name);
      String hotelReservationId = activities.bookHotel(hotelReservationRequestID, name);

      String flightReservationRequestID = Workflow.randomUUID().toString();
      saga.addCompensation(activities::cancelFlight, flightReservationRequestID, name);
      String flightReservationID = activities.bookFlight(flightReservationRequestID, name);
      return new Booking(carReservationID, hotelReservationId, flightReservationID);
    } catch (ActivityFailure e) {
      // Ensure that compensations are executed even if the workflow is canceled.
      Workflow.newDetachedCancellationScope(() -> saga.compensate()).run();
      throw e;
    }
  }
}
