package io.temporal.samples.bookingsyncsaga;

public final class Booking {
  private final String carReservationID;
  private final String hotelReservationID;
  private final String flightReservationID;

  public Booking(String carReservationID, String hotelReservationID, String flightReservationID) {
    this.carReservationID = carReservationID;
    this.hotelReservationID = hotelReservationID;
    this.flightReservationID = flightReservationID;
  }

  public String getCarReservationID() {
    return carReservationID;
  }

  public String getHotelReservationID() {
    return hotelReservationID;
  }

  public String getFlightReservationID() {
    return flightReservationID;
  }

  @Override
  public String toString() {
    return "Booking{"
        + "carReservationID='"
        + carReservationID
        + '\''
        + ", hotelReservationID='"
        + hotelReservationID
        + '\''
        + ", flightReservationID='"
        + flightReservationID
        + '\''
        + '}';
  }
}
