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

package io.temporal.samples.bookingsyncsaga;

import io.temporal.activity.ActivityOptions;
import io.temporal.activity.LocalActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.CompletablePromise;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class TripBookingWorkflowImpl implements TripBookingWorkflow {

  /**
   * Use local activities for the happy path. This allows to execute the whole sequence as a single
   * workflow task. Don't use local activities if you expect long retries.
   */
  private final LocalActivityOptions options =
      LocalActivityOptions.newBuilder()
          .build()
          .newBuilder()
          .setStartToCloseTimeout(Duration.ofSeconds(1))
          .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
          .build();

  private final TripBookingActivities activities =
      Workflow.newLocalActivityStub(TripBookingActivities.class, options);

  /** Use normal activities for compensations, as they potentially need long retries. */
  private final ActivityOptions compensationOptions =
      ActivityOptions.newBuilder()
          .setStartToCloseTimeout(Duration.ofHours(1))
          .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
          .build();

  private final TripBookingActivities compensationActivities =
      Workflow.newActivityStub(TripBookingActivities.class, compensationOptions);

  /** Used to pass result to the update function. */
  private final CompletablePromise<Booking> booking = Workflow.newPromise();

  @Override
  public void bookTrip(String name) {
    Saga.Options sagaOptions = new Saga.Options.Builder().build();
    Saga saga = new Saga(sagaOptions);
    try {
      // addCompensation is added before the actual call to handle situations when the call failed
      // due to
      // a timeout and its success is not clear.
      // The compensation code must handle situations when the actual function wasn't executed
      // gracefully.
      String carReservationRequestId = Workflow.randomUUID().toString();
      saga.addCompensation(compensationActivities::cancelCar, carReservationRequestId, name);
      String carReservationID = activities.reserveCar(carReservationRequestId, name);

      String hotelReservationRequestID = Workflow.randomUUID().toString();
      saga.addCompensation(compensationActivities::cancelHotel, hotelReservationRequestID, name);
      String hotelReservationId = activities.bookHotel(hotelReservationRequestID, name);

      String flightReservationRequestID = Workflow.randomUUID().toString();
      saga.addCompensation(compensationActivities::cancelFlight, flightReservationRequestID, name);
      String flightReservationID = activities.bookFlight(flightReservationRequestID, name);

      // Unblock the update function
      booking.complete(new Booking(carReservationID, hotelReservationId, flightReservationID));
    } catch (ActivityFailure e) {
      // Unblock the update function
      booking.completeExceptionally(e);
      // Ensure that compensations are executed even if the workflow is canceled.
      Workflow.newDetachedCancellationScope(() -> saga.compensate()).run();
      throw e;
    }
  }

  @Override
  public Booking waitForBooking() {
    return booking.get();
  }
}
