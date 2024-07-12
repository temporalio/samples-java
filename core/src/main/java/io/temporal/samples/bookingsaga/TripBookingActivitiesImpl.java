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

import io.temporal.failure.ApplicationFailure;
import java.util.UUID;

public class TripBookingActivitiesImpl implements TripBookingActivities {
  @Override
  public String reserveCar(String requestId, String name) {
    System.out.println("reserving car for request '" + requestId + "` and name `" + name + "'");
    return UUID.randomUUID().toString();
  }

  @Override
  public String bookFlight(String requestId, String name) {
    System.out.println(
        "failing to book flight for request '" + requestId + "' and name '" + name + "'");
    throw ApplicationFailure.newNonRetryableFailure(
        "Flight booking did not work", "bookingFailure");
  }

  @Override
  public String bookHotel(String requestId, String name) {
    System.out.println("booking hotel for request '" + requestId + "` and name `" + name + "'");
    return UUID.randomUUID().toString();
  }

  @Override
  public String cancelFlight(String requestId, String name) {
    System.out.println("cancelling flight reservation '" + requestId + "' for '" + name + "'");
    return UUID.randomUUID().toString();
  }

  @Override
  public String cancelHotel(String requestId, String name) {
    System.out.println("cancelling hotel reservation '" + requestId + "' for '" + name + "'");
    return UUID.randomUUID().toString();
  }

  @Override
  public String cancelCar(String requestId, String name) {
    System.out.println("cancelling car reservation '" + requestId + "' for '" + name + "'");
    return UUID.randomUUID().toString();
  }
}
