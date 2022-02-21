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

package io.temporal.samples.peractivityoptions;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableMap;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.RetryOptions;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkflowImplementationOptions;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;

public class PerActivityOptionsTest {
  WorkflowImplementationOptions options =
      WorkflowImplementationOptions.newBuilder()
          .setActivityOptions(
              ImmutableMap.of(
                  "ActivityTypeA",
                  ActivityOptions.newBuilder()
                      .setScheduleToCloseTimeout(Duration.ofSeconds(5))
                      .build(),
                  "ActivityTypeB",
                  ActivityOptions.newBuilder()
                      .setStartToCloseTimeout(Duration.ofSeconds(2))
                      .setRetryOptions(
                          RetryOptions.newBuilder()
                              .setDoNotRetry(NullPointerException.class.getName())
                              .build())
                      .build()))
          .build();

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(options, PerActivityOptionsWorkflowImpl.class)
          .setActivityImplementations(new FailingActivitiesImpl())
          .build();

  @Test
  public void testPerActivityTypeWorkflow() {
    PerActivityOptionsWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                PerActivityOptionsWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    WorkflowStub untyped = WorkflowStub.fromTyped(workflow);
    WorkflowExecution execution = untyped.start();
    // wait until workflow completes
    untyped.getResult(Void.class);

    DescribeWorkflowExecutionResponse resp =
        testWorkflowRule
            .getWorkflowClient()
            .getWorkflowServiceStubs()
            .blockingStub()
            .describeWorkflowExecution(
                DescribeWorkflowExecutionRequest.newBuilder()
                    .setNamespace(testWorkflowRule.getTestEnvironment().getNamespace())
                    .setExecution(execution)
                    .build());

    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        resp.getWorkflowExecutionInfo().getStatus());
  }
}
