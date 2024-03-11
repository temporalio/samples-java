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

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class HelloSignalWithTimerTest {
  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloSignalWithTimer.SignalWithTimerWorkflowImpl.class)
          .setActivityImplementations(new HelloSignalWithTimer.ValueProcessingActivitiesImpl())
          .build();

  private static final String WORKFLOW_ID = "SignalWithTimerTestWorkflow";

  @Test
  public void testSignalWithTimer() {
    HelloSignalWithTimer.SignalWithTimerWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloSignalWithTimer.SignalWithTimerWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .setWorkflowId(WORKFLOW_ID)
                    .build());

    WorkflowClient.start(workflow::execute);
    workflow.newValue("1");
    workflow.newValue("2");
    workflow.exit();

    WorkflowStub.fromTyped(workflow).getResult(Void.class);

    DescribeWorkflowExecutionResponse res =
        testWorkflowRule
            .getWorkflowClient()
            .getWorkflowServiceStubs()
            .blockingStub()
            .describeWorkflowExecution(
                DescribeWorkflowExecutionRequest.newBuilder()
                    .setNamespace(testWorkflowRule.getTestEnvironment().getNamespace())
                    .setExecution(WorkflowExecution.newBuilder().setWorkflowId(WORKFLOW_ID).build())
                    .build());

    Assert.assertEquals(
        res.getWorkflowExecutionInfo().getStatus(),
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED);
  }
}
