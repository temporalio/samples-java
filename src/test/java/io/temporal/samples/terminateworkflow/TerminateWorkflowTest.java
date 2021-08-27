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

package io.temporal.samples.terminateworkflow;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.TerminatedFailure;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class TerminateWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder().setWorkflowTypes(MyWorkflowImpl.class).build();

  @Test
  public void testTerminateWorkflow() {
    WorkflowStub wfs =
        testWorkflowRule
            .getWorkflowClient()
            .newUntypedWorkflowStub(
                "MyWorkflow",
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    wfs.start(testWorkflowRule.getTaskQueue());
    try {
      Thread.sleep(1000);
    } catch (Exception e) {
      fail(e.getMessage());
    }
    wfs.terminate("Test Reasons");
    try {
      wfs.getResult(String.class);
      fail("unreachable");
    } catch (WorkflowFailedException ignored) {
      assertTrue(ignored.getCause() instanceof TerminatedFailure);
      assertEquals("Test Reasons", ((TerminatedFailure) ignored.getCause()).getOriginalMessage());
    }
  }
}
