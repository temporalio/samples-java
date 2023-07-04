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

package io.temporal.samples.polling.infrequent;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.samples.polling.PollingActivities;
import io.temporal.samples.polling.PollingWorkflow;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class InfrequentPollingWorkflowImpl implements PollingWorkflow {
  @Override
  public String exec() {
    /*
     * Infrequent polling via activity can be implemented via activity retries. For this sample we
     * want to poll the test service every 60 seconds. Here we:
     *
     * <ol>
     *   <li>Set RetryPolicy backoff coefficient of 1
     *   <li>Set initial interval to the poll frequency (since coefficient is 1, same interval will
     *       be used for all retries)
     * </ol>
     *
     * <p>With this in case our test service is "down" we can fail our activity and it will be
     * retried based on our 60 second retry interval until poll is successful and we can return a
     * result from the activity.
     */
    ActivityOptions options =
        ActivityOptions.newBuilder()
            // Set activity StartToClose timeout (single activity exec), does not include retries
            .setStartToCloseTimeout(Duration.ofSeconds(2))
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setBackoffCoefficient(1)
                    .setInitialInterval(Duration.ofSeconds(60))
                    .build())
            .build();
    // create our activities stub and start activity execution
    PollingActivities activities = Workflow.newActivityStub(PollingActivities.class, options);
    return activities.doPoll();
  }
}
