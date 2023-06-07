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

import java.util.Set;

/** Used as a result of {@link SlidingWindowBatchWorkflow#getProgress()} query. */
public final class BatchProgress {

  private final int progress;

  private final Set<Integer> currentRecords;

  public BatchProgress(int progress, Set<Integer> currentRecords) {
    this.progress = progress;
    this.currentRecords = currentRecords;
  }

  /** Count of completed record processing child workflows. */
  public int getProgress() {
    return progress;
  }

  /** Ids of records that are currently being processed by child workflows. */
  public Set<Integer> getCurrentRecords() {
    return currentRecords;
  }
}
