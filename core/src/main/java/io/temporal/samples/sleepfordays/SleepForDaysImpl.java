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

package io.temporal.samples.sleepfordays;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class SleepForDaysImpl implements SleepForDaysWorkflow {

  private final SendEmailActivity activity;
  private boolean complete = false;

  public SleepForDaysImpl() {
    this.activity =
        Workflow.newActivityStub(
            SendEmailActivity.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());
  }

  @Override
  public String sleepForDays() {
    while (!this.complete) {
      activity.sendEmail(String.format("Sleeping for 30 days"));
      Promise<Void> timer = Workflow.newTimer(Duration.ofDays(30));
      Workflow.await(() -> timer.isCompleted() || this.complete);
    }

    return "done!";
  }

  @Override
  public void complete() {
    this.complete = true;
  }
}
