package io.temporal.samples.bookingsaga;

import io.temporal.failure.ApplicationFailure;
import java.util.UUID;

public class TripBookingActivitiesImpl implements TripBookingActivities {
  @Override
  public String reserveCar(String requestId, String name) {
    System.out.println("reserving car for request '" + requestId + "` and name `" + name + "'");
    return UUID.randomUUID().toString();
  }

  @Override
  public String bookFlight(String requestId, String name) {
    System.out.println(
        "failing to book flight for request '" + requestId + "' and name '" + name + "'");
    throw ApplicationFailure.newNonRetryableFailure(
        "Flight booking did not work", "bookingFailure");
  }

  @Override
  public String bookHotel(String requestId, String name) {
    System.out.println("booking hotel for request '" + requestId + "` and name `" + name + "'");
    return UUID.randomUUID().toString();
  }

  @Override
  public String cancelFlight(String requestId, String name) {
    System.out.println("cancelling flight reservation '" + requestId + "' for '" + name + "'");
    return UUID.randomUUID().toString();
  }

  @Override
  public String cancelHotel(String requestId, String name) {
    System.out.println("cancelling hotel reservation '" + requestId + "' for '" + name + "'");
    return UUID.randomUUID().toString();
  }

  @Override
  public String cancelCar(String requestId, String name) {
    System.out.println("cancelling car reservation '" + requestId + "' for '" + name + "'");
    return UUID.randomUUID().toString();
  }
}
