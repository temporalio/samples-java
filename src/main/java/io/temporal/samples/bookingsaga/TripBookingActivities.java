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

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface TripBookingActivities {

  /**
   * Request a car rental reservation.
   *
   * @param name customer name
   * @return reservationID
   */
  String reserveCar(String name);

  /**
   * Request a flight reservation.
   *
   * @param name customer name
   * @return reservationID
   */
  String bookFlight(String name);

  /**
   * Request a hotel reservation.
   *
   * @param name customer name
   * @return reservationID
   */
  String bookHotel(String name);

  /**
   * Cancel a flight reservation.
   *
   * @param name customer name
   * @param reservationID id returned by bookFlight
   * @return cancellationConfirmationID
   */
  String cancelFlight(String reservationID, String name);

  /**
   * Cancel a hotel reservation.
   *
   * @param name customer name
   * @param reservationID id returned by bookHotel
   * @return cancellationConfirmationID
   */
  String cancelHotel(String reservationID, String name);

  /**
   * Cancel a car rental reservation.
   *
   * @param name customer name
   * @param reservationID id returned by reserveCar
   * @return cancellationConfirmationID
   */
  String cancelCar(String reservationID, String name);
}
