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

package io.temporal.samples.batch.slidingwindow;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Implements BatchWorkflow by running multiple SlidingWindowBatchWorkflows in parallel. */
public class BatchWorkflowImpl implements BatchWorkflow {

  private final RecordLoader recordLoader =
      Workflow.newActivityStub(
          RecordLoader.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

  @Override
  public int processBatch(int pageSize, int slidingWindowSize, int partitions) {
    // The sample partitions the data set into continuous ranges.
    // A real application can choose any other way to divide the records into multiple collections.
    int totalCount = recordLoader.getRecordCount();
    int partitionSize = totalCount / partitions + (totalCount % partitions > 0 ? 1 : 0);
    List<Promise<Integer>> results = new ArrayList<>(partitions);
    for (int i = 0; i < partitions; i++) {
      // Makes child id more user-friendly
      String childId = Workflow.getInfo().getWorkflowId() + "/" + i;
      SlidingWindowBatchWorkflow partitionWorkflow =
          Workflow.newChildWorkflowStub(
              SlidingWindowBatchWorkflow.class,
              ChildWorkflowOptions.newBuilder().setWorkflowId(childId).build());
      // Define partition boundaries.
      int offset = partitionSize * i;
      int maximumOffset = Math.min(offset + partitionSize, totalCount);

      ProcessBatchInput input = new ProcessBatchInput();
      input.setPageSize(pageSize);
      input.setSlidingWindowSize(slidingWindowSize);
      input.setOffset(offset);
      input.setMaximumOffset(maximumOffset);

      Promise<Integer> partitionResult = Async.function(partitionWorkflow::processBatch, input);
      results.add(partitionResult);
    }
    int result = 0;
    for (Promise<Integer> partitionResult : results) {
      result += partitionResult.get();
    }
    return result;
  }
}
