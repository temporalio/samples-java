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

import static io.temporal.samples.batch.iterator.RecordLoaderImpl.PAGE_COUNT;
import static org.junit.Assert.assertTrue;

import io.temporal.testing.TestWorkflowRule;
import io.temporal.workflow.Workflow;
import org.junit.Rule;
import org.junit.Test;

public class IteratorIteratorBatchWorkflowTest {

  private static final int PAGE_SIZE = 10;
  /** The sample RecordLoaderImpl always returns the fixed number pages. */
  private static boolean[] processedRecords = new boolean[PAGE_SIZE * PAGE_COUNT];

  public static class TestRecordProcessorWorkflowImpl implements RecordProcessorWorkflow {

    @Override
    public void processRecord(SingleRecord r) {
      Workflow.sleep(5000);
      processedRecords[r.getId()] = true;
    }
  }

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(IteratorBatchWorkflowImpl.class, TestRecordProcessorWorkflowImpl.class)
          .setActivityImplementations(new RecordLoaderImpl())
          .build();

  @Test
  public void testBatchWorkflow() {
    IteratorBatchWorkflow workflow = testWorkflowRule.newWorkflowStub(IteratorBatchWorkflow.class);
    workflow.processBatch(PAGE_SIZE, 0);

    for (int i = 0; i < processedRecords.length; i++) {
      assertTrue(processedRecords[i]);
    }
  }
}
