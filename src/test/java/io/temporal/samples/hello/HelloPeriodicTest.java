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

import static io.temporal.samples.hello.HelloPeriodic.PERIODIC_WORKFLOW_ID;
import static io.temporal.samples.hello.HelloPeriodic.TASK_QUEUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.v1.WorkflowExecution;
import io.temporal.enums.v1.WorkflowExecutionStatus;
import io.temporal.filter.v1.WorkflowExecutionFilter;
import io.temporal.samples.hello.HelloPeriodic.GreetingActivities;
import io.temporal.samples.hello.HelloPeriodic.GreetingActivitiesImpl;
import io.temporal.samples.hello.HelloPeriodic.GreetingWorkflow;
import io.temporal.samples.hello.HelloPeriodic.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.workflow.v1.WorkflowExecutionInfo;
import io.temporal.workflowservice.v1.ListClosedWorkflowExecutionsRequest;
import io.temporal.workflowservice.v1.ListClosedWorkflowExecutionsResponse;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;

/** Unit test for {@link HelloPeriodic}. Doesn't use an external Temporal service. */
public class HelloPeriodicTest {

  @Rule public Timeout globalTimeout = Timeout.seconds(2);

  /** Prints a history of the workflow under test in case of a test failure. */
  @Rule
  public TestWatcher watchman =
      new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
          if (testEnv != null) {
            System.err.println(testEnv.getDiagnostics());
            testEnv.close();
          }
        }
      };

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  private WorkflowClient client;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    client = testEnv.getWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testPeriodicActivityInvocation() {
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
    testEnv.start();

    // Get a workflow stub using the same task queue the worker uses.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId(PERIODIC_WORKFLOW_ID)
                .build());
    // Execute a workflow waiting for it to complete.
    WorkflowExecution execution = WorkflowClient.start(workflow::greetPeriodically, "World");
    assertEquals(PERIODIC_WORKFLOW_ID, execution.getWorkflowId());
    // Validate that workflow was continued as new at least once.
    // Use TestWorkflowEnvironment.sleep to execute the unit test without really sleeping.
    testEnv.sleep(Duration.ofMinutes(3));
    ListClosedWorkflowExecutionsRequest request =
        ListClosedWorkflowExecutionsRequest.newBuilder()
            .setNamespace(testEnv.getNamespace())
            .setExecutionFilter(
                WorkflowExecutionFilter.newBuilder().setWorkflowId(PERIODIC_WORKFLOW_ID))
            .build();
    ListClosedWorkflowExecutionsResponse listResponse =
        testEnv.getWorkflowService().blockingStub().listClosedWorkflowExecutions(request);
    assertTrue(listResponse.getExecutionsCount() > 1);
    for (WorkflowExecutionInfo e : listResponse.getExecutionsList()) {
      assertEquals(
          WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_CONTINUED_AS_NEW, e.getStatus());
    }
  }

  @Test
  public void testMockedActivity() {
    GreetingActivities activities = mock(GreetingActivities.class);
    worker.registerActivitiesImplementations(activities);
    testEnv.start();

    // Get a workflow stub using the same task queue the worker uses.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId(PERIODIC_WORKFLOW_ID)
                .build());
    // Execute a workflow waiting for it to complete.
    WorkflowExecution execution = WorkflowClient.start(workflow::greetPeriodically, "World");
    assertEquals(PERIODIC_WORKFLOW_ID, execution.getWorkflowId());
    // Use TestWorkflowEnvironment.sleep to execute the unit test without really sleeping.
    testEnv.sleep(Duration.ofMinutes(1));
    verify(activities, atLeast(5)).greet(anyString());
  }
}
