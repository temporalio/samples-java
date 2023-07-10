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

package io.temporal.samples.springboot.customize;

import io.temporal.activity.ActivityOptions;
import io.temporal.activity.LocalActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.TimeoutFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;

/**
 * In our custom config we have set that worker polling on CustomizeTaskQueue to be a "local
 * activity worker", meaning it would not poll for activity tasks. For this sample we will try to
 * start an activity as "normal" activity which should time out, then invoke it again as local which
 * should be successful.
 *
 * @see io.temporal.samples.springboot.customize.TemporalOptionsConfig
 */
@WorkflowImpl(taskQueues = "CustomizeTaskQueue")
public class CustomizeWorkflowImpl implements CustomizeWorkflow {
  private CustomizeActivity asNormalActivity =
      Workflow.newActivityStub(
          CustomizeActivity.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(2))
              .setScheduleToCloseTimeout(Duration.ofSeconds(4))
              .build());

  private CustomizeActivity asLocalActivity =
      Workflow.newLocalActivityStub(
          CustomizeActivity.class,
          LocalActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());
  private Logger logger = Workflow.getLogger(CustomizeActivity.class.getName());

  @Override
  public String execute() {
    try {
      return asNormalActivity.run("Normal");
    } catch (ActivityFailure e) {
      // We should have TimeoutFailure as activity failure cause with StartToClose timeout type
      TimeoutFailure tf = (TimeoutFailure) e.getCause();
      logger.warn("asNormalActivity failed with timeout type: " + tf.getTimeoutType());
    }
    return asLocalActivity.run("Local");
  }
}
