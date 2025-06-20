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

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.client.ActivityCanceledException;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptorBase;
import io.temporal.samples.autoheartbeat.AutoHeartbeatUtil;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AutoHeartbeatActivityInboundCallsInterceptor
    extends ActivityInboundCallsInterceptorBase {
  private ActivityExecutionContext activityExecutionContext;
  private Duration activityHeartbeatTimeout;
  private AutoHeartbeatUtil autoHeartbeater;
  //  private CompletableFuture autoHeartbeatFuture;
  private ScheduledFuture scheduledFuture;

  //  private ScheduledFuture scheduledFuture;

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
  @SuppressWarnings({"FutureReturnValueIgnored", "CatchAndPrintStackTrace"})
  public ActivityOutput execute(ActivityInput input) {
    // If activity has heartbeat timeout defined we want to apply auto-heartbeter
    if (activityHeartbeatTimeout != null && activityHeartbeatTimeout.getSeconds() > 0) {
      System.out.println(
          "Auto heartbeating applied for activity: "
              + activityExecutionContext.getInfo().getActivityType());
      autoHeartbeater =
          new AutoHeartbeatUtil(2, 0, TimeUnit.SECONDS, activityExecutionContext, input);
      scheduledFuture = autoHeartbeater.start();
    } else {
      System.out.println(
          "Auto heartbeating not being applied for activity: "
              + activityExecutionContext.getInfo().getActivityType());
    }

    if (scheduledFuture != null) {
      CompletableFuture activityExecFuture =
          CompletableFuture.supplyAsync(() -> super.execute(input));
      CompletableFuture autoHeartbeatFuture =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  return scheduledFuture.get();
                } catch (Exception e) {
                  throw new ActivityCanceledException(activityExecutionContext.getInfo());
                }
              });
      try {
        CompletableFuture.anyOf(autoHeartbeatFuture, activityExecFuture).get();
      } catch (Exception e) {
        if (e instanceof ExecutionException) {
          ExecutionException ee = (ExecutionException) e;
          if (ee.getCause() instanceof ActivityCanceledException) {
            throw new ActivityCanceledException(activityExecutionContext.getInfo());
          }
        }
        throw Activity.wrap(e);
      } finally {
        if (autoHeartbeater != null) {
          autoHeartbeater.stop();
        }
      }
    }
    return super.execute(input);
  }

  public interface AutoHeartbeaterCancellationCallback {
    void handle(Exception e);
  }
}
