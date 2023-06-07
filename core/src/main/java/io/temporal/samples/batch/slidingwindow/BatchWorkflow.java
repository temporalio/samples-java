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

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface BatchWorkflow {

  /**
   * Processes a batch of records using multiple parallel sliding window workflows.
   *
   * @param pageSize the number of records to start processing in a single sliding window workflow
   *     run.
   * @param slidingWindowSize the number of records to process in parallel by a single sliding
   *     window workflow. Can be larger than the pageSize.
   * @param partitions defines the number of SlidingWindowBatchWorkflows to run in parallel. If
   *     number of partitions is too low the update rate of a single SlidingWindowBatchWorkflows can
   *     get too high.
   * @return total number of processed records.
   */
  @WorkflowMethod
  int processBatch(int pageSize, int slidingWindowSize, int partitions);
}
