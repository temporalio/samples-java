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
