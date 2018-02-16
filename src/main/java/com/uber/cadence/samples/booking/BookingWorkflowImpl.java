/*
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.uber.cadence.samples.booking;

import com.uber.cadence.workflow.ActivityOptions;
import com.uber.cadence.workflow.Workflow;

public class BookingWorkflowImpl implements BookingWorkflow {

    @Override
    public void makeBooking(String activityTaskList, int requestID, int customerID, boolean reserveAir, boolean reserveCar) {
        // Demonstrates how to use task list which name is provided at runtime.
        ActivityOptions options = new ActivityOptions.Builder()
                .setTaskList(activityTaskList)
                .setScheduleToCloseTimeoutSeconds(30)
                .build();
        BookingActivities activities = Workflow.newActivityStub(BookingActivities.class, options);

        activities.reserveCar(requestID);
        if (reserveAir) {
            activities.reserveAirline(requestID);
        }
        activities.sendConfirmationActivity(customerID);
    }
}
