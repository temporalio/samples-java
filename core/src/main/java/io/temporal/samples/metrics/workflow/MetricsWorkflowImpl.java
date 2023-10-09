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

package io.temporal.samples.metrics.workflow;

import com.uber.m3.tally.Scope;
import io.temporal.activity.ActivityOptions;
import io.temporal.samples.metrics.activities.MetricsActivities;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Collections;

public class MetricsWorkflowImpl implements MetricsWorkflow {

  private final MetricsActivities activities =
      Workflow.newActivityStub(
          MetricsActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

  @Override
  public String exec(String input) {
    /*
     * Custom metric, we can use child scope and attach workflow_id as it's not attached by default
     * like task_queue ,workflow_type, etc
     */
    Scope scope =
        Workflow.getMetricsScope()
            .tagged(Collections.singletonMap("workflow_id", Workflow.getInfo().getWorkflowId()));
    scope.counter("custom_metric").inc(1);

    String result = activities.performA(input);
    Workflow.sleep(Duration.ofSeconds(5));
    result += activities.performB(input);

    return result;
  }
}
