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

package io.temporal.samples.polling.infrequentwithretryafter;

import io.temporal.activity.Activity;
import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.polling.PollingActivities;
import io.temporal.samples.polling.TestService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class InfrequentPollingWithRetryAfterActivityImpl implements PollingActivities {
  private TestService service;
  final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

  public InfrequentPollingWithRetryAfterActivityImpl(TestService service) {
    this.service = service;
  }

  @Override
  public String doPoll() {
    System.out.println(
        "Attempt: "
            + Activity.getExecutionContext().getInfo().getAttempt()
            + " Poll time: "
            + LocalDateTime.now(ZoneId.systemDefault()).format(ISO_FORMATTER));

    try {
      return service.getServiceResult();
    } catch (TestService.TestServiceException e) {
      // we throw application failure that includes cause
      // which is the test service exception
      // and delay which is the interval to next retry based on test service retry-after directive
      System.out.println("Activity next retry in: " + e.getRetryAfterInMinutes() + " minutes");
      throw ApplicationFailure.newFailureWithCauseAndDelay(
          e.getMessage(),
          e.getClass().getName(),
          e,
          // here we set the next retry interval based on Retry-After duration given to us by our
          // service
          Duration.ofMinutes(e.getRetryAfterInMinutes()));
    }
  }
}
