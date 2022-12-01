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

package io.temporal.samples.batch.heartbeatingactivity;

import static io.temporal.samples.batch.heartbeatingactivity.RecordLoaderImpl.RECORD_COUNT;
import static org.junit.Assert.assertTrue;

import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class HeartbeatingActivityBatchWorkflowTest {
  private static boolean[] processedRecords = new boolean[RECORD_COUNT];

  public static class TestRecordProcessorImpl implements RecordProcessor {

    @Override
    public void processRecord(SingleRecord r) {
      processedRecords[r.getId()] = true;
    }
  }

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HeartbeatingActivityBatchWorkflowImpl.class)
          .setActivityImplementations(
              new RecordProcessorActivityImpl(
                  new RecordLoaderImpl(), new TestRecordProcessorImpl()))
          .build();

  @Test
  public void testBatchWorkflow() {
    HeartbeatingActivityBatchWorkflow workflow =
        testWorkflowRule.newWorkflowStub(HeartbeatingActivityBatchWorkflow.class);
    workflow.processBatch();

    for (int i = 0; i < processedRecords.length; i++) {
      assertTrue(processedRecords[i]);
    }
  }
}
