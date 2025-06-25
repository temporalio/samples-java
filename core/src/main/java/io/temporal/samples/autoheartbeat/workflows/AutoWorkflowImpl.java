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

package io.temporal.samples.autoheartbeat.workflows;

import io.temporal.activity.ActivityCancellationType;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.failure.TimeoutFailure;
import io.temporal.samples.autoheartbeat.activities.AutoActivities;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class AutoWorkflowImpl implements AutoWorkflow {
  private CancellationScope scope;

  @Override
  public String exec(String input) {
    AutoActivities activitiesOne =
        Workflow.newActivityStub(
            AutoActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(22))
                .setHeartbeatTimeout(Duration.ofSeconds(8))
                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                // for sample purposes
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());

    AutoActivities activitiesTwo =
        Workflow.newActivityStub(
            AutoActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(20))
                .setHeartbeatTimeout(Duration.ofSeconds(7))
                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                // for sample purposes
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());

    // Start our activity in CancellationScope so we can cancel it if needed
    scope =
        Workflow.newCancellationScope(
            () -> {
              activitiesOne.runActivityOne(input);
              activitiesTwo.runActivityTwo(input);
            });

    try {
      scope.run();
    } catch (ActivityFailure e) {
      if (e.getCause() instanceof CanceledFailure) {
        // We dont want workflow to fail in we canceled our scope, just log and return
        return "Workflow result after activity cancellation";
      } else if (e.getCause() instanceof TimeoutFailure) {
        return "Workflow result after activity timeout of type: "
            + ((TimeoutFailure) e.getCause()).getTimeoutType().name();
      } else {
        throw e;
      }
    }
    return "completed";
  }

  @Override
  public void cancelActivity() {
    scope.cancel("Canceling scope from signal handler");
  }
}
