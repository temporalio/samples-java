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

import static com.uber.cadence.samples.common.SampleConstants.DOMAIN;

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowException;
import com.uber.cadence.worker.Worker;

public class TripBookingSaga {

  static final String TASK_LIST = "TripBooking";

  @SuppressWarnings("CatchAndPrintStackTrace")
  public static void main(String[] args) {
    // Get worker to poll the common task list.
    Worker.Factory factory = new Worker.Factory(DOMAIN);
    final Worker workerForCommonTaskList = factory.newWorker(TASK_LIST);
    workerForCommonTaskList.registerWorkflowImplementationTypes(TripBookingWorkflowImpl.class);
    TripBookingActivities tripBookingActivities = new TripBookingActivitiesImpl();
    workerForCommonTaskList.registerActivitiesImplementations(tripBookingActivities);

    // Start all workers created by this factory.
    factory.start();
    System.out.println("Worker started for task list: " + TASK_LIST);

    WorkflowClient workflowClient = WorkflowClient.newInstance(DOMAIN);

    // now we can start running instances of our saga - its state will be persisted
    TripBookingWorkflow trip1 = workflowClient.newWorkflowStub(TripBookingWorkflow.class);
    try {
      trip1.bookTrip("trip1");
    } catch (WorkflowException e) {
      // Expected
      e.printStackTrace();
    }

    try {
      TripBookingWorkflow trip2 = workflowClient.newWorkflowStub(TripBookingWorkflow.class);
      trip2.bookTrip("trip2");
    } catch (WorkflowException e) {
      e.printStackTrace();
    }

    System.exit(0);
  }
}
