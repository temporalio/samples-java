/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

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
