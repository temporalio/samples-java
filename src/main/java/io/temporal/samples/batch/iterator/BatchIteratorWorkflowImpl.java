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

package io.temporal.samples.batch.iterator;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements iterator workflow pattern.
 *
 * <p>A single workflow run processes a single page of records in parallel. Each record is processed
 * using its own BatchWorkflow child workflow.
 *
 * <p>After all child workflows complete the new run of the parent workflow is created using
 * continue as new. The new run processes the next page of records. This way practically unlimited
 * set of records can be processed.
 */
public final class BatchIteratorWorkflowImpl implements BatchWorkflow {

  private final RecordLoader recordLoader =
      Workflow.newActivityStub(
          RecordLoader.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

  private final BatchWorkflow nextRun = Workflow.newContinueAsNewStub(BatchWorkflow.class);

  @Override
  public int processBatch(int pageSize, int offset) {
    List<Record> records = recordLoader.getRecords(pageSize, offset);
    List<Promise<Void>> results = new ArrayList<>(records.size());
    for (Record record : records) {
      RecordProcessorWorkflow processor =
          Workflow.newChildWorkflowStub(RecordProcessorWorkflow.class);
      Promise<Void> result = Async.procedure(processor::processRecord, record);
      results.add(result);
    }
    // Wait for all promises to resolve
    Promise.allOf(results).get();

    // Skipped error handling for the sample brevity.
    // So failed RecordProcessorWorkflows are ignored.

    // No more records in the dataset.
    if (records.isEmpty()) {
      return offset + records.size();
    }

    // Continue as new with the increased offset.
    return nextRun.processBatch(pageSize, offset + records.size());
  }
}
