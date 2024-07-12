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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowException;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowUpdateException;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Assertions;
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
    Assertions.assertEquals(
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

    // Starts workflow asynchronously.
    WorkflowClient.start(workflow::bookTrip, "trip1");

    // Waits for update to return.
    WorkflowException exception1 =
        assertThrows(WorkflowUpdateException.class, () -> workflow.waitForBooking());
    Assertions.assertEquals(
        "Flight booking did not work",
        ((ApplicationFailure) exception1.getCause().getCause()).getOriginalMessage());

    // Waits for workflow to complete.
    WorkflowStub stub = WorkflowStub.fromTyped(workflow);
    WorkflowException exception2 =
        assertThrows(WorkflowException.class, () -> stub.getResult(Void.class));
    Assertions.assertEquals(
        "Flight booking did not work",
        ((ApplicationFailure) exception2.getCause().getCause()).getOriginalMessage());

    verify(activities).cancelHotel(captorHotelRequestId.getValue(), "trip1");
    verify(activities).cancelCar(captorCarRequestId.getValue(), "trip1");
    verify(activities).cancelFlight(captorFlightRequestId.getValue(), "trip1");
  }
}
