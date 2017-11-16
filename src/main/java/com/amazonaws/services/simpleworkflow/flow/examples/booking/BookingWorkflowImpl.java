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

import com.amazonaws.services.simpleworkflow.flow.ActivitySchedulingOptions;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;

public class BookingWorkflowImpl implements BookingWorkflow {

    private final BookingActivitiesClient client = new BookingActivitiesClientImpl();

    @Override
    public void makeBooking(String activityTaskList, int requestID, int customerID, boolean reserveAir, boolean reserveCar) {
        ActivitySchedulingOptions options = client.getSchedulingOptions();
        options.setTaskList(activityTaskList);
        options.setScheduleToCloseTimeoutSeconds(30);
        options.setScheduleToStartTimeoutSeconds(10);
        options.setStartToCloseTimeoutSeconds(20);
        options.setHeartbeatTimeoutSeconds(10);

        Promise<Void> carReservation = null;
        if (reserveCar) {
            carReservation = client.reserveCar(requestID);
        }
        Promise<Void> airReservation = null;
        if (reserveAir) {
            airReservation = client.reserveAirline(requestID);
        }
        // Relies on null Promise parameter considered immediately ready
        client.sendConfirmationActivity(customerID, carReservation, airReservation);
    }
    
}
