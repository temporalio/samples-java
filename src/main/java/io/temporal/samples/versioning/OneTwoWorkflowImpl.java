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

package io.temporal.samples.versioning;

import static io.temporal.samples.versioning.OneTwoWorker.TASK_QUEUE;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class OneTwoWorkflowImpl implements OneTwoWorkflow {

  private boolean signaled;

  @Override
  public int run() {
    ActivityOptions options =
        ActivityOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setScheduleToCloseTimeout(Duration.ofSeconds(10))
            .build();
    OneTwoActivities activities = Workflow.newActivityStub(OneTwoActivities.class, options);

    int number = activities.one();
    //    int number = activities.two(); // TODO uncomment while workflow is running to see
    // non-deterministic error after the signal.

    Workflow.await(() -> signaled);

    return number;
  }

  @Override
  public void signal() {
    signaled = true;
  }
}
