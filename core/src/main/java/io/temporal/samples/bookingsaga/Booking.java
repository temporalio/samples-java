

package io.temporal.samples.bookingsaga;

public final class Booking {
  private String carReservationID;
  private String hotelReservationID;
  private String flightReservationID;

  /** Empty constructor to keep Jackson serializer happy. */
  public Booking() {}

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
