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

package io.temporal.samples.autoheartbeat.interceptor;

import io.temporal.activity.ActivityExecutionContext;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptorBase;
import io.temporal.samples.autoheartbeat.AutoHeartbeatUtil;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class AutoHeartbeatActivityInboundCallsInterceptor
    extends ActivityInboundCallsInterceptorBase {
  private ActivityExecutionContext activityExecutionContext;
  private Duration activityHeartbeatTimeout;

  public AutoHeartbeatActivityInboundCallsInterceptor(ActivityInboundCallsInterceptor next) {
    super(next);
  }

  @Override
  public void init(ActivityExecutionContext context) {
    this.activityExecutionContext = context;
    activityHeartbeatTimeout = activityExecutionContext.getInfo().getHeartbeatTimeout();
    super.init(context);
  }

  @Override
  @SuppressWarnings("FutureReturnValueIgnored")
  public ActivityOutput execute(ActivityInput input) {
    // If activity has heartbeat timeout defined we want to apply auto-heartbeter
    AutoHeartbeatUtil autoHearbeater = null;
    if (activityHeartbeatTimeout != null && activityHeartbeatTimeout.getSeconds() > 0) {
      System.out.println(
          "Auto heartbeating applied for activity: "
              + activityExecutionContext.getInfo().getActivityType());
      autoHearbeater =
          new AutoHeartbeatUtil(
              getHeartbeatPeriod(activityHeartbeatTimeout),
              0,
              TimeUnit.SECONDS,
              activityExecutionContext,
              input);
      autoHearbeater.start();
    } else {
      System.out.println(
          "Auto heartbeating not being applied for activity: "
              + activityExecutionContext.getInfo().getActivityType());
    }

    try {
      return super.execute(input);
    } catch (Exception e) {
      throw e;
    } finally {
      if (autoHearbeater != null) {
        autoHearbeater.stop();
      }
    }
  }

  private long getHeartbeatPeriod(Duration activityHeartbeatTimeout) {
    // For sample we want to heartbeat 1 seconds less than heartbeat timeout
    return activityHeartbeatTimeout.getSeconds() <= 1
        ? 1
        : activityHeartbeatTimeout.getSeconds() - 1;
  }
}
