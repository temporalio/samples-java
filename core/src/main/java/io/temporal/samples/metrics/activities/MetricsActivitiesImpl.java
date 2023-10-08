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

package io.temporal.samples.metrics.activities;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;

public class MetricsActivitiesImpl implements MetricsActivities {

  @Override
  public String performA(String input) {
    // simulate some failures to trigger retries
    if (Activity.getExecutionContext().getInfo().getAttempt() < 3) {
      incRetriesCustomMetric(Activity.getExecutionContext());
      throw Activity.wrap(new NullPointerException("simulated"));
    }
    return "Performed activity A with input " + input + "\n";
  }

  @Override
  public String performB(String input) {
    // simulate some failures to trigger retries
    if (Activity.getExecutionContext().getInfo().getAttempt() < 5) {
      incRetriesCustomMetric(Activity.getExecutionContext());
      throw Activity.wrap(new NullPointerException("simulated"));
    }
    return "Performed activity B with input " + input + "\n";
  }

  private void incRetriesCustomMetric(ActivityExecutionContext context) {
    // We can create a child scope and add extra tags
    //    Scope scope =
    //        context
    //            .getMetricsScope()
    //            .tagged(
    //                Stream.of(
    //                        new String[][] {
    //                          {"workflow_id", context.getInfo().getWorkflowId()},
    //                          {"activity_id", context.getInfo().getActivityId()},
    //                          {
    //                            "activity_start_to_close_timeout",
    //                            context.getInfo().getStartToCloseTimeout().toString()
    //                          },
    //                        })
    //                    .collect(Collectors.toMap(data -> data[0], data -> data[1])));
    //
    //    scope.counter("custom_activity_retries").inc(1);

    // For sample we use root scope
    context.getMetricsScope().counter("custom_activity_retries").inc(1);
  }
}
