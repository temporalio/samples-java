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

import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Optional;
import java.util.Random;
import org.slf4j.Logger;

/** Fake RecordProcessorWorkflow implementation. */
public final class RecordProcessorWorkflowImpl implements RecordProcessorWorkflow {
  public static final Logger log = Workflow.getLogger(RecordProcessorWorkflowImpl.class);
  private final Random random = Workflow.newRandom();

  @Override
  public void processRecord(SingleRecord r) {
    processRecordImpl(r);
    // This workflow is always expected to have a parent.
    // But for unit testing it might be useful to skip the notification.
    Optional<String> parentWorkflowId = Workflow.getInfo().getParentWorkflowId();
    if (parentWorkflowId.isPresent()) {
      String parentId = parentWorkflowId.get();
      SlidingWindowBatchWorkflow parent =
          Workflow.newExternalWorkflowStub(SlidingWindowBatchWorkflow.class, parentId);
      // Notify parent about record processing completion
      parent.reportCompletion(r.getId());
    }
  }

  /** Application specific record processing logic goes here. */
  private void processRecordImpl(SingleRecord r) {
    // Simulate some processing
    Workflow.sleep(Duration.ofSeconds(random.nextInt(10)));
    log.info("Processed " + r);
  }
}
