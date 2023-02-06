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

package io.temporal.samples.polling.periodicsequence;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.samples.polling.PollingActivities;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class PeriodicPollingChildWorkflowImpl implements PollingChildWorkflow {

  private int singleWorkflowPollAttempts = 10;

  @Override
  public String exec(int pollingIntervalInSeconds) {
    PollingActivities activities =
        Workflow.newActivityStub(
            PollingActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(4))
                // Explicitly disable default retries for activities
                // as activity retries are handled with business logic in this case
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                .build());

    for (int i = 0; i < singleWorkflowPollAttempts; i++) {
      // Here we would invoke a sequence of activities
      // For sample we just use a single one
      try {
        return activities.doPoll();
      } catch (ActivityFailure e) {
        // Log error after retries exhausted
      }
      // Sleep for a second
      Workflow.sleep(Duration.ofSeconds(1));
    }

    // Request that the new child workflow run is invoked
    PollingChildWorkflow continueAsNew = Workflow.newContinueAsNewStub(PollingChildWorkflow.class);
    continueAsNew.exec(pollingIntervalInSeconds);
    // unreachable
    return null;
  }
}
