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

package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.testing.TestWorkflowRule;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;

public class HelloDelayedStartTest {
  private final String WORKFLOW_ID = "HelloDelayedStartWorkflow";

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloDelayedStart.DelayedStartWorkflowImpl.class)
          .build();

  @Test
  public void testDelayedStart() {
    // Get a workflow stub using the same task queue the worker uses.
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowId(WORKFLOW_ID)
            .setStartDelay(Duration.ofSeconds(2))
            .build();

    HelloDelayedStart.DelayedStartWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(HelloDelayedStart.DelayedStartWorkflow.class, workflowOptions);

    workflow.start();

    // Fetch event history and make sure we got the 2 seconds first workflow task backoff
    WorkflowExecutionHistory history =
        testWorkflowRule.getWorkflowClient().fetchHistory(WORKFLOW_ID);
    com.google.protobuf.Duration backoff =
        history
            .getHistory()
            .getEvents(0)
            .getWorkflowExecutionStartedEventAttributes()
            .getFirstWorkflowTaskBackoff();

    assertEquals(2, backoff.getSeconds());
  }
}
