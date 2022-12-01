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

package io.temporal.samples.batch.heartbeatingactivity;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * A sample implementation of processing a batch by an activity.
 *
 * <p>An activity can run as long as needed. It reports that it is still alive through heartbeat. If
 * the worker is restarted the activity is retried after the heartbeat timeout. Temporal allows
 * store data in heartbeat _details_. These details are available to the next activity attempt. The
 * progress of the record processing is stored in the details to avoid reprocessing records from the
 * beginning on failures.
 */
public final class HeartbeatingActivityBatchWorkflowImpl
    implements HeartbeatingActivityBatchWorkflow {

  /**
   * Activity that is used to process batch records. The start-to-close timeout is set to a high
   * value to support large batch sizes. Heartbeat timeout is required to quickly restart the
   * activity in case of failures. The heartbeat timeout is also needed to record heartbeat details
   * at the service.
   */
  private final RecordProcessorActivity recordProcessor =
      Workflow.newActivityStub(
          RecordProcessorActivity.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofHours(1))
              .setHeartbeatTimeout(Duration.ofSeconds(10))
              .build());

  @Override
  public int processBatch() {
    // No special logic needed here as activity is retried automatically by the service.
    return recordProcessor.processRecords();
  }
}
