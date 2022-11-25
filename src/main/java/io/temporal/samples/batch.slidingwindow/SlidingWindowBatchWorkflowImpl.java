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

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.util.*;

/**
 * Implements batch processing by executing a specified number of workflows in parallel. A new
 * record processing workflow is started when a previously started one completes. The child
 * completion is reported through reportCompletion signal as it is not yet possible to passively
 * wait for a workflow that was started by a previous run.
 *
 * <p>Calls continue-as-new after starting 100 children. Note that the sliding window size can be
 * larger than 100.
 */
public final class SlidingWindowBatchWorkflowImpl implements BatchWorkflow {

  /** Defines how frequently continue-as-new is called. */
  private static final int MAX_CHILDREN_PER_RUN = 100;

  /** Stub used to call continue-as-new. */
  private final BatchWorkflow nextRun = Workflow.newContinueAsNewStub(BatchWorkflow.class);

  /** Contains ids of records that are being processed by child workflows. */
  private Set<Integer> currentRecords;

  /** Count of completed record processing child workflows. */
  private int progress;

  /** Uses ParentClosePolicy ABANDON to ensure that children survive continue-as-new of a parent. */
  private ChildWorkflowOptions childWorkflowOptions =
      ChildWorkflowOptions.newBuilder()
          .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
          .build();

  /**
   * @param pageSize the number of records to load in a single RecordLoader.getRecords call.
   * @param slidingWindowSize the number of parallel record processing child workflows to execute.
   * @param offset the offset of the first record to process. 0 to start the batch processing.
   * @param progress the number of completed record processing child workflows.
   * @param currentRecords currentRecords contains a set of record ids of currently running
   *     workflows. This puts a limit on the sliding window size as workflow arguments cannot exceed
   *     2MB in JSON format. Another practical limit is the number of signals a workflow can handle
   *     per second. If this rate exceeds a few per second then run multiple such sliding window
   *     workflows in parallel for a single batch job.
   * @return number of processed records
   */
  @Override
  public int processBatch(
      int pageSize, int slidingWindowSize, int offset, int progress, Set<Integer> currentRecords) {
    this.progress = progress;
    this.currentRecords = currentRecords;

    Iterable<Record> records = new RecordIterable(pageSize, offset);
    List<Promise<WorkflowExecution>> childrenStartedByThisRun = new ArrayList<>();

    Iterator<Record> recordIterator = records.iterator();
    while (true) {
      // After starting slidingWindowSize children blocks until the completion signal is received.
      Workflow.await(() -> currentRecords.size() < slidingWindowSize);
      // Starts missing children in parallel.
      for (int i = 0; i < slidingWindowSize - currentRecords.size(); i++) {
        // Completes workflow, if no more records to process.
        if (!recordIterator.hasNext()) {
          // Awaits for all children to complete
          Workflow.await(() -> currentRecords.size() == 0);
          return offset + childrenStartedByThisRun.size();
        }
        Record record = recordIterator.next();
        RecordProcessorWorkflow processor =
            Workflow.newChildWorkflowStub(RecordProcessorWorkflow.class, childWorkflowOptions);
        // Starts a child workflow asynchronously ignoring its result.
        // The assumption is that the parent workflow doesn't need to deal with child workflow
        // results and failures. Another assumption is that a child in any situation calls
        // the reportCompletion signal.
        Async.procedure(processor::processRecord, record);
        // Resolves when a child reported successful start.
        // Used to wait for a child start on continue-as-new.
        Promise<WorkflowExecution> childStartedPromise = Workflow.getWorkflowExecution(processor);
        childrenStartedByThisRun.add(childStartedPromise);
        currentRecords.add(record.getId());
        if (childrenStartedByThisRun.size() == MAX_CHILDREN_PER_RUN) {
          // Waits for all children to start. Without this wait, workflow completion through
          // continue-as-new might lead to a situation when they never start.
          // Assumes that they never fail to start as their automatically generated
          // IDs are not expected to collide.
          Promise.allOf(childrenStartedByThisRun).get();
          // Continues as new to keep the history size bounded
          return nextRun.processBatch(
              pageSize,
              slidingWindowSize,
              offset + childrenStartedByThisRun.size(),
              this.progress,
              currentRecords);
        }
      }
    }
  }

  @Override
  public void reportCompletion(int recordId) {
    // Dedupes signals as in some edge cases they can be duplicated.
    if (currentRecords.remove(recordId)) {
      progress++;
    }
  }

  @Override
  public BatchProgress getProgress() {
    return new BatchProgress(progress, currentRecords);
  }
}
