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

package io.temporal.samples.autoheartbeat.activities;

import io.temporal.client.ActivityCompletionException;
import java.util.concurrent.TimeUnit;

public class AutoActivitiesImpl implements AutoActivities {

  @Override
  public String runActivityOne(String input) {
    return runActivity("runActivityOne - " + input, 20);
  }

  @Override
  public String runActivityTwo(String input) {
    return runActivity("runActivityTwo - " + input, 10);
  }

  @Override
  public String runActivityThree(String input) {
    return runActivity("runActivityThree - " + input, 3);
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private String runActivity(String input, int seconds) {
    for (int i = 0; i < seconds; i++) {
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
        // We want to rethrow the cancel failure
        throw e;
      }
    }
    return "Activity completed: " + input;
  }

  private void sleep(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException ee) {
      // Empty
    }
  }
}
