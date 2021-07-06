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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.temporal.client.WorkflowException;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class TripBookingWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(TripBookingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  /**
   * Not very useful test that validates that the default activities cause workflow to fail. See
   * other tests on using mocked activities to test SAGA logic.
   */
  @Test
  public void testTripBookingFails() {
    testWorkflowRule.getWorker().registerActivitiesImplementations(new TripBookingActivitiesImpl());
    testWorkflowRule.getTestEnvironment().start();

    TripBookingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                TripBookingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    try {
      workflow.bookTrip("trip1");
      fail("unreachable");
    } catch (WorkflowException e) {
      assertEquals(
          "Flight booking did not work",
          ((ApplicationFailure) e.getCause().getCause()).getOriginalMessage());
    }

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  /** Unit test workflow logic using mocked activities. */
  @Test
  public void testSAGA() {
    TripBookingActivities activities = mock(TripBookingActivities.class);
    when(activities.bookHotel("trip1")).thenReturn("HotelBookingID1");
    when(activities.reserveCar("trip1")).thenReturn("CarBookingID1");
    when(activities.bookFlight("trip1"))
        .thenThrow(new RuntimeException("Flight booking did not work"));
    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);

    testWorkflowRule.getTestEnvironment().start();

    TripBookingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                TripBookingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    try {
      workflow.bookTrip("trip1");
      fail("unreachable");
    } catch (WorkflowException e) {
      assertEquals(
          "Flight booking did not work",
          ((ApplicationFailure) e.getCause().getCause()).getOriginalMessage());
    }

    verify(activities).cancelHotel(eq("HotelBookingID1"), eq("trip1"));
    verify(activities).cancelCar(eq("CarBookingID1"), eq("trip1"));

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
