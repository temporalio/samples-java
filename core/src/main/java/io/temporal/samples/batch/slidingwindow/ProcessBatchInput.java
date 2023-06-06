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

import java.util.HashSet;
import java.util.Set;

/** Input of {@link SlidingWindowBatchWorkflow#processBatch(ProcessBatchInput)} */
public final class ProcessBatchInput {
  private int pageSize;
  private int slidingWindowSize;

  int offset;

  private int maximumOffset;

  private int progress;

  private Set<Integer> currentRecords = new HashSet<>();

  /** the number of records to load in a single RecordLoader.getRecords call. */
  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  /** the number of parallel record processing child workflows to execute. */
  public void setSlidingWindowSize(int slidingWindowSize) {
    this.slidingWindowSize = slidingWindowSize;
  }

  /** index of the first record to process. 0 to start the batch processing. */
  public void setOffset(int offset) {
    this.offset = offset;
  }

  /** The maximum offset (exclusive) to process by this workflow. */
  public void setMaximumOffset(int maximumOffset) {
    this.maximumOffset = maximumOffset;
  }

  /** Total number of records processed so far by this workflow. */
  public void setProgress(int progress) {
    this.progress = progress;
  }

  /**
   * Ids of records that are being processed by child workflows.
   *
   * <p>This puts a limit on the sliding window size as workflow arguments cannot exceed 2MB in JSON
   * format. Another practical limit is the number of signals a workflow can handle per second.
   * Adjust the number of partitions to keep this rate at a reasonable value.
   */
  public void setCurrentRecords(Set<Integer> currentRecords) {
    this.currentRecords = currentRecords;
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getSlidingWindowSize() {
    return slidingWindowSize;
  }

  public int getOffset() {
    return offset;
  }

  public int getMaximumOffset() {
    return maximumOffset;
  }

  public int getProgress() {
    return progress;
  }

  public Set<Integer> getCurrentRecords() {
    return currentRecords;
  }
}
