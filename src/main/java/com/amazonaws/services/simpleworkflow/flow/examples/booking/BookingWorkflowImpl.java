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
package com.amazonaws.services.simpleworkflow.flow.examples.booking;

import com.uber.cadence.workflow.ActivitySchedulingOptions;
import com.uber.cadence.workflow.Workflow;

public class BookingWorkflowImpl implements BookingWorkflow {

    @Override
    public void makeBooking(String activityTaskList, int requestID, int customerID, boolean reserveAir, boolean reserveCar) {
        BookingActivities client = getBookingActivities(activityTaskList);

        client.reserveCar(requestID);
        if (reserveAir) {
            client.reserveAirline(requestID);
        }
        client.sendConfirmationActivity(customerID);
    }

    private BookingActivities getBookingActivities(String activityTaskList) {
        ActivitySchedulingOptions options = new ActivitySchedulingOptions();
        options.setTaskList(activityTaskList);
        options.setScheduleToCloseTimeoutSeconds(30);
        options.setScheduleToStartTimeoutSeconds(10);
        options.setStartToCloseTimeoutSeconds(20);
        options.setHeartbeatTimeoutSeconds(10);
        return Workflow.newActivityClient(BookingActivities.class, options);
    }

}
