/*
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

package com.uber.cadence.samples.bookingsaga;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowException;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TripBookingWorkflowTest {

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  private WorkflowClient workflowClient;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(TripBookingSaga.TASK_LIST);
    worker.registerWorkflowImplementationTypes(TripBookingWorkflowImpl.class);

    workflowClient = testEnv.newWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  /**
   * Not very useful test that validates that the default activities cause workflow to fail. See
   * other tests on using mocked activities to test SAGA logic.
   */
  @Test
  public void testTripBookingFails() {
    worker.registerActivitiesImplementations(new TripBookingActivitiesImpl());
    testEnv.start();

    TripBookingWorkflow workflow = workflowClient.newWorkflowStub(TripBookingWorkflow.class);
    try {
      workflow.bookTrip("trip1");
      fail("unreachable");
    } catch (WorkflowException e) {
      assertEquals("Flight booking did not work", e.getCause().getCause().getMessage());
    }
  }

  /** Unit test workflow logic using mocked activities. */
  @Test
  public void testSAGA() {
    TripBookingActivities activities = mock(TripBookingActivities.class);
    when(activities.bookHotel("trip1")).thenReturn("HotelBookingID1");
    when(activities.reserveCar("trip1")).thenReturn("CarBookingID1");
    when(activities.bookFlight("trip1"))
        .thenThrow(new RuntimeException("Flight booking did not work"));
    worker.registerActivitiesImplementations(activities);

    testEnv.start();

    TripBookingWorkflow workflow = workflowClient.newWorkflowStub(TripBookingWorkflow.class);
    try {
      workflow.bookTrip("trip1");
      fail("unreachable");
    } catch (WorkflowException e) {
      assertEquals("Flight booking did not work", e.getCause().getCause().getMessage());
    }

    verify(activities).cancelHotel(eq("HotelBookingID1"), eq("trip1"));
    verify(activities).cancelCar(eq("CarBookingID1"), eq("trip1"));
  }
}
