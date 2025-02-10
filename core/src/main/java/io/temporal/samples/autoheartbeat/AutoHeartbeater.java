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

package io.temporal.samples.autoheartbeat;

import io.temporal.activity.ActivityExecutionContext;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AutoHeartbeater {
  private final long period;
  private final long initialDelay;
  private final TimeUnit periodTimeUnit;
  private final ScheduledExecutorService timerService =
      Executors.newSingleThreadScheduledExecutor();
  private final ActivityExecutionContext context;
  private final Object details;
  private String heartbeaterId;

  public AutoHeartbeater(
      long period,
      long initialDelay,
      TimeUnit periodTimeUnit,
      ActivityExecutionContext context,
      Object details) {
    this.period = period;
    this.initialDelay = initialDelay;
    this.periodTimeUnit = periodTimeUnit;
    this.context = context;
    this.details = details;
    // Set to activity id better, for sample we just use type
    heartbeaterId = context.getInfo().getActivityType();
  }

  public ScheduledFuture<?> start() {
    System.out.println("Autoheartbeater[" + heartbeaterId + "] starting...");
    return timerService.scheduleAtFixedRate(
        () -> {
          try {
            System.out.println(
                "Autoheartbeater["
                    + heartbeaterId
                    + "]"
                    + "heartbeating at: "
                    + printShortCurrentTime());
            context.heartbeat(details);
          } catch (Exception e) {
            System.out.println("Stopping: " + e.getMessage());
            stop();
          }
        },
        initialDelay,
        period,
        periodTimeUnit);
  }

  public void stop() {
    System.out.println("Autoheartbeater being requested to stop.");
    // Try not to execute another heartbeat that could have been queued up
    timerService.shutdownNow();
  }

  private String printShortCurrentTime() {
    return DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(Instant.now());
  }
}
