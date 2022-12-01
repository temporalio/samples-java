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

import static org.junit.Assert.assertTrue;

import io.temporal.testing.TestWorkflowRule;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInfo;
import org.junit.Rule;
import org.junit.Test;

public class SlidingWindowBatchWorkflowTest {

  private static final int RECORD_COUNT = 15;
  private static boolean[] processedRecords = new boolean[RECORD_COUNT];

  public static class TestRecordProcessorWorkflowImpl implements RecordProcessorWorkflow {

    @Override
    public void processRecord(SingleRecord r) {
      processedRecords[r.getId()] = true;
      WorkflowInfo info = Workflow.getInfo();
      String parentId = info.getParentWorkflowId().get();
      SlidingWindowBatchWorkflow parent =
          Workflow.newExternalWorkflowStub(SlidingWindowBatchWorkflow.class, parentId);
      Workflow.sleep(500);
      // Notify parent about record processing completion
      // Needs to retry due to a continue-as-new atomicity
      // bug in the testservice:
      // https://github.com/temporalio/sdk-java/issues/1538
      while (true) {
        try {
          parent.reportCompletion(r.getId());
          break;
        } catch (Exception e) {
          continue;
        }
      }
    }
  }

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(
              SlidingWindowBatchWorkflowImpl.class, TestRecordProcessorWorkflowImpl.class)
          .setActivityImplementations(new RecordLoaderImpl())
          .build();

  @Test
  public void testSlidingWindowBatchWorkflow() {
    SlidingWindowBatchWorkflow workflow =
        testWorkflowRule.newWorkflowStub(SlidingWindowBatchWorkflow.class);

    ProcessBatchInput input = new ProcessBatchInput();
    input.setPageSize(3);
    input.setSlidingWindowSize(7);
    input.setOffset(0);
    input.setMaximumOffset(RECORD_COUNT);
    workflow.processBatch(input);
    for (int i = 0; i < RECORD_COUNT; i++) {
      assertTrue(processedRecords[i]);
    }
  }
}
