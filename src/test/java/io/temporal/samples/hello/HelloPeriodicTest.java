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

import static io.temporal.samples.hello.HelloPeriodic.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.filter.v1.WorkflowExecutionFilter;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.ListClosedWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListClosedWorkflowExecutionsResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloPeriodic.GreetingActivities;
import io.temporal.samples.hello.HelloPeriodic.GreetingActivitiesImpl;
import io.temporal.samples.hello.HelloPeriodic.GreetingWorkflow;
import io.temporal.samples.hello.HelloPeriodic.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloPeriodic}. Doesn't use an external Temporal service. */
public class HelloPeriodicTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(GreetingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testPeriodicActivityInvocation() {
    testWorkflowRule.getWorker().registerActivitiesImplementations(new GreetingActivitiesImpl());
    testWorkflowRule.getTestEnvironment().start();

    // Get a workflow stub using the same task queue the worker uses.
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .setWorkflowId(WORKFLOW_ID)
                    .build());
    // Execute a workflow waiting for it to complete.
    WorkflowExecution execution = WorkflowClient.start(workflow::greetPeriodically, "World");
    assertEquals(WORKFLOW_ID, execution.getWorkflowId());
    // Validate that workflow was continued as new at least once.
    // Use TestWorkflowEnvironment.sleep to execute the unit test without really sleeping.
    testWorkflowRule.getTestEnvironment().sleep(Duration.ofMinutes(3));
    ListClosedWorkflowExecutionsRequest request =
        ListClosedWorkflowExecutionsRequest.newBuilder()
            .setNamespace(testWorkflowRule.getTestEnvironment().getNamespace())
            .setExecutionFilter(WorkflowExecutionFilter.newBuilder().setWorkflowId(WORKFLOW_ID))
            .build();
    ListClosedWorkflowExecutionsResponse listResponse =
        testWorkflowRule
            .getTestEnvironment()
            .getWorkflowService()
            .blockingStub()
            .listClosedWorkflowExecutions(request);
    assertTrue(listResponse.getExecutionsCount() > 1);
    for (WorkflowExecutionInfo e : listResponse.getExecutionsList()) {
      assertEquals(
          WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_CONTINUED_AS_NEW, e.getStatus());
    }

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test
  public void testMockedActivity() {
    GreetingActivities activities = mock(GreetingActivities.class);
    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);
    testWorkflowRule.getTestEnvironment().start();

    // Get a workflow stub using the same task queue the worker uses.
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .setWorkflowId(WORKFLOW_ID)
                    .build());
    // Execute a workflow waiting for it to complete.
    WorkflowExecution execution = WorkflowClient.start(workflow::greetPeriodically, "World");
    assertEquals(WORKFLOW_ID, execution.getWorkflowId());
    // Use TestWorkflowEnvironment.sleep to execute the unit test without really sleeping.
    testWorkflowRule.getTestEnvironment().sleep(Duration.ofMinutes(1));
    verify(activities, atLeast(5)).greet(anyString());

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
