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

package io.temporal.samples.updatabletimer;

import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;

public final class UpdatableTimer {

  private final Logger logger = Workflow.getLogger(UpdatableTimer.class);

  private long wakeUpTime;
  private boolean wakeUpTimeUpdated;

  public void sleepUntil(long wakeUpTime) {
    Instant wakeUpInstant = Instant.ofEpochMilli(wakeUpTime);
    LocalDateTime date = wakeUpInstant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    logger.info("sleepUntil: " + date);
    this.wakeUpTime = wakeUpTime;
    while (true) {
      wakeUpTimeUpdated = false;
      Duration sleepInterval = Duration.ofMillis(this.wakeUpTime - Workflow.currentTimeMillis());
      logger.info("Going to sleep for " + sleepInterval);
      if (!Workflow.await(sleepInterval, () -> wakeUpTimeUpdated)) {
        break;
      }
    }
    logger.info("sleepUntil completed");
  }

  public void updateWakeUpTime(long wakeUpTime) {
    this.wakeUpTime = wakeUpTime;
    this.wakeUpTimeUpdated = true;
  }

  public long getWakeUpTime() {
    return wakeUpTime;
  }
}
