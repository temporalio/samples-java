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

package io.temporal.samples.autoheartbeat;

import io.temporal.activity.Activity;
import io.temporal.client.ActivityCompletionException;
import java.util.concurrent.TimeUnit;

public class AutoActivitiesImpl implements AutoActivities {

  @Override
  public String runActivityOne(String input) {
    return runActivity("runActivityOne - " + input);
  }

  @Override
  public String runActivityTwo(String input) {
    return runActivity("runActivityTwo - " + input);
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private String runActivity(String input) {
    // Calculate heartbeat period based on our heartbeat timeout
    // Start Autoheartbeater
    AutoHeartbeater autoHearbeater =
        new AutoHeartbeater(
            getHeartbeatPeriod(), 0, TimeUnit.SECONDS, Activity.getExecutionContext(), input);
    autoHearbeater.start();

    // For sample our activity just sleeps for a second for 20 seconds
    for (int i = 0; i < 20; i++) {
      try {
        sleep(1);
      } catch (ActivityCompletionException e) {
        System.out.println(
            "Activity type:"
                + e.getActivityType().get()
                + "Activiy id: "
                + e.getActivityId().get()
                + "Workflow id: "
                + e.getWorkflowId().get()
                + "Workflow runid: "
                + e.getRunId().get()
                + " was canceled. Shutting down auto heartbeats");
        autoHearbeater.stop();
        // We want to rethrow the cancel failure
        throw e;
      }
    }
    return "Activity completed: " + input;
  }

  private long getHeartbeatPeriod() {
    // Note you can add checks if heartbeat timeout is set if not and
    // decide to log / fail activity / not start autoheartbeater based on your business logic

    // For sample we want to heartbeat 1 seconds less than heartbeat timeout
    return Activity.getExecutionContext().getInfo().getHeartbeatTimeout().getSeconds() <= 1
        ? 1
        : Activity.getExecutionContext().getInfo().getHeartbeatTimeout().getSeconds() - 1;
  }

  private void sleep(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException ee) {
      // Empty
    }
  }
}
