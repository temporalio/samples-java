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
   * Process the batch of records using multiple parallel sliding window workflows.
   *
   * @param pageSize the number of records to start processing in a single workflow run.
   * @param slidingWindowSize the number of records to process in parallel. Can be larger than
   *     pageSize.
   * @param partitions defines the number of SlidingWindowBatchWorkflows to run in parallel.
   * @return total number of processed records.
   */
  @WorkflowMethod
  int processBatch(int pageSize, int slidingWindowSize, int partitions);
}
