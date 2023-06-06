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
import io.temporal.workflow.*;
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
public final class SlidingWindowBatchWorkflowImpl implements SlidingWindowBatchWorkflow {

  /** Stub used to call continue-as-new. */
  private final SlidingWindowBatchWorkflow nextRun =
      Workflow.newContinueAsNewStub(SlidingWindowBatchWorkflow.class);

  /** Contains ids of records that are being processed by child workflows. */
  private Set<Integer> currentRecords;

  /**
   * Used to accumulate records to remove for signals delivered before processBatch method started
   * execution
   */
  private Set<Integer> recordsToRemove = new HashSet<>();

  /** Count of completed record processing child workflows. */
  private int progress;

  /**
   * @return number of processed records
   */
  @Override
  public int processBatch(ProcessBatchInput input) {
    WorkflowInfo info = Workflow.getInfo();
    this.progress = input.getProgress();
    this.currentRecords = input.getCurrentRecords();
    // Remove records for signals delivered before the workflow run started.
    int countBefore = this.currentRecords.size();
    this.currentRecords.removeAll(recordsToRemove);
    this.progress += countBefore - this.currentRecords.size();

    int pageSize = input.getPageSize();
    int offset = input.getOffset();
    int maximumOffset = input.getMaximumOffset();
    int slidingWindowSize = input.getSlidingWindowSize();

    Iterable<SingleRecord> records = new RecordIterable(pageSize, offset, maximumOffset);
    List<Promise<WorkflowExecution>> childrenStartedByThisRun = new ArrayList<>();

    Iterator<SingleRecord> recordIterator = records.iterator();
    while (true) {
      // After starting slidingWindowSize children blocks until a completion signal is received.
      Workflow.await(() -> currentRecords.size() < slidingWindowSize);
      // Completes workflow, if no more records to process.
      if (!recordIterator.hasNext()) {
        // Awaits for all children to complete
        Workflow.await(() -> currentRecords.size() == 0);
        return offset + childrenStartedByThisRun.size();
      }
      SingleRecord record = recordIterator.next();

      // Uses ParentClosePolicy ABANDON to ensure that children survive continue-as-new of a parent.
      // Assigns user-friendly child workflow id.
      ChildWorkflowOptions childWorkflowOptions =
          ChildWorkflowOptions.newBuilder()
              .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
              .setWorkflowId(info.getWorkflowId() + "/" + record.getId())
              .build();

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
      // Continues-as-new after starting pageSize children
      if (childrenStartedByThisRun.size() == pageSize) {
        // Waits for all children to start. Without this wait, workflow completion through
        // continue-as-new might lead to a situation when they never start.
        // Assumes that they never fail to start as their automatically generated
        // IDs are not expected to collide.
        Promise.allOf(childrenStartedByThisRun).get();
        // Continues as new to keep the history size bounded
        ProcessBatchInput newInput = new ProcessBatchInput();
        newInput.setPageSize(pageSize);
        newInput.setSlidingWindowSize(slidingWindowSize);
        newInput.setOffset(offset + childrenStartedByThisRun.size());
        newInput.setMaximumOffset(maximumOffset);
        newInput.setProgress(progress);
        newInput.setCurrentRecords(currentRecords);
        return nextRun.processBatch(newInput);
      }
    }
  }

  @Override
  public void reportCompletion(int recordId) {
    // Handle situation when signal handler is called before the workflow main method.
    if (currentRecords == null) {
      recordsToRemove.add(recordId);
      return;
    }
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
