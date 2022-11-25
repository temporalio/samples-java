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
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Implements BatchWorkflow by running multiple SlidingWindowBatchWorkflows in parallel. */
public class BatchWorkflowImpl implements BatchWorkflow {

  private final RecordLoader recordLoader =
      Workflow.newActivityStub(
          RecordLoader.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

  @Override
  public int processBatch(int pageSize, int slidingWindowSize, int partitions) {
    int totalCount = recordLoader.getRecordCount();
    int partitionSize = totalCount / partitions;
    List<Promise<Integer>> results = new ArrayList<>(partitions);
    for (int i = 0; i < partitions; i++) {
      SlidingWindowBatchWorkflow partitionWorkflow =
          Workflow.newChildWorkflowStub(SlidingWindowBatchWorkflow.class);
      // Define partition boundaries.
      int offset = partitionSize * i;
      int maximumOffset = partitionSize * (i + 1);

      ProcessBatchInput input = new ProcessBatchInput();
      input.setPageSize(pageSize);
      input.setSlidingWindowSize(slidingWindowSize);
      input.setOffset(offset);
      input.setMaximumOffset(maximumOffset);
      input.setCurrentRecords(new HashSet<>());

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
