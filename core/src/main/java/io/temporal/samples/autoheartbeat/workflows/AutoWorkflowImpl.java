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

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.samples.autoheartbeat.activities.AutoActivities;
import io.temporal.workflow.Async;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AutoWorkflowImpl implements AutoWorkflow {
  private CancellationScope scope;

  @Override
  public String exec(String input) {
    // Crete separate workflow stubs for same interface so we can show
    // use of different heartbeat timeouts and activit that does not heartbeat
    // Note you can do this also via WorkflowImplementationOptions instead of using different
    // activity stubs if you wanted
    AutoActivities activitiesOne =
        Workflow.newActivityStub(
            AutoActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(22))
                .setHeartbeatTimeout(Duration.ofSeconds(5))
                .build());

    AutoActivities activitiesTwo =
        Workflow.newActivityStub(
            AutoActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(12))
                .setHeartbeatTimeout(Duration.ofSeconds(3))
                .build());

    // Activity three does not heartbeat so autoheartbeat should not be applied to it
    AutoActivities activitiesThree =
        Workflow.newActivityStub(
            AutoActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

    // Start our activity in CancellationScope so we can cancel it if needed
    List<Promise<String>> activityPromises = new ArrayList<>();
    scope =
        Workflow.newCancellationScope(
            () -> {
              activityPromises.add(Async.function(activitiesOne::runActivityOne, input));
              activityPromises.add(Async.function(activitiesTwo::runActivityTwo, input));
              activityPromises.add(Async.function(activitiesThree::runActivityThree, input));
            });

    scope.run();

    try {
      Promise.allOf(activityPromises).get();
      String result = "";
      for (Promise<String> pr : activityPromises) {
        result += pr.get();
      }
      return result;
    } catch (ActivityFailure e) {
      if (e.getCause() instanceof CanceledFailure) {
        // We dont want workflow to fail in we canceled our scope, just log and return
        return "Workflow result after activity cancellation";
      } else {
        // We want to fail execution on any other failures except cancellation
        throw e;
      }
    }
  }

  @Override
  public void cancelActivity() {
    scope.cancel("Canceling scope from signal handler");
  }
}
