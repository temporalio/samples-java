

package io.temporal.samples.bookingsaga;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface TripBookingActivities {

  /**
   * Request a car rental reservation.
   *
   * @param requestId used for idempotency and compensation correlation.
   * @param name customer name
   * @return reservationID
   */
  String reserveCar(String requestId, String name);

  /**
   * Request a flight reservation.
   *
   * @param requestId used for idempotency and compensation correlation.
   * @param name customer name
   * @return reservationID
   */
  String bookFlight(String requestId, String name);

  /**
   * Request a hotel reservation.
   *
   * @param requestId used for idempotency and compensation correlation.
   * @param name customer name
   * @return reservationID
   */
  String bookHotel(String requestId, String name);

  /**
   * Cancel a flight reservation.
   *
   * @param name customer name
   * @param requestId the same id is passed to bookFlight
   * @return cancellationConfirmationID
   */
  String cancelFlight(String requestId, String name);

  /**
   * Cancel a hotel reservation.
   *
   * @param name customer name
   * @param requestId the same id is passed to bookHotel
   * @return cancellationConfirmationID
   */
  String cancelHotel(String requestId, String name);

  /**
   * Cancel a car rental reservation.
   *
   * @param name customer name
   * @param requestId the same id is passed to reserveCar
   * @return cancellationConfirmationID
   */
  String cancelCar(String requestId, String name);
}
