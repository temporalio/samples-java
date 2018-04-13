/*
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

package com.uber.cadence.samples.hello;

import static com.uber.cadence.samples.hello.HelloPeriodic.PERIODIC_WORKFLOW_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.uber.cadence.ListClosedWorkflowExecutionsRequest;
import com.uber.cadence.ListClosedWorkflowExecutionsResponse;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowExecutionCloseStatus;
import com.uber.cadence.WorkflowExecutionFilter;
import com.uber.cadence.WorkflowExecutionInfo;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.samples.hello.HelloPeriodic.GreetingActivities;
import com.uber.cadence.samples.hello.HelloPeriodic.GreetingActivitiesImpl;
import com.uber.cadence.samples.hello.HelloPeriodic.GreetingWorkflow;
import com.uber.cadence.samples.hello.HelloPeriodic.GreetingWorkflowImpl;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import java.time.Duration;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;

/** Unit test for {@link HelloPeriodic}. Doesn't use an external Cadence service. */
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
  private WorkflowClient workflowClient;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(HelloPeriodic.TASK_LIST);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    workflowClient = testEnv.newWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testPeriodicActivityInvocation() throws TException {
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
    worker.start();

    // Get a workflow stub using the same task list the worker uses.
    GreetingWorkflow workflow = workflowClient.newWorkflowStub(GreetingWorkflow.class);
    // Execute a workflow waiting for it to complete.
    WorkflowExecution execution =
        WorkflowClient.start(workflow::greetPeriodically, "World", Duration.ofSeconds(1));
    assertEquals(PERIODIC_WORKFLOW_ID, execution.getWorkflowId());
    // Validate that workflow was continued as new at least once.
    // Use TestWorkflowEnvironment.sleep to execute the unit test without really sleeping.
    testEnv.sleep(Duration.ofMinutes(1));
    ListClosedWorkflowExecutionsRequest request =
        new ListClosedWorkflowExecutionsRequest()
            .setDomain(testEnv.getDomain())
            .setExecutionFilter(new WorkflowExecutionFilter().setWorkflowId(PERIODIC_WORKFLOW_ID));
    ListClosedWorkflowExecutionsResponse listResponse =
        testEnv.getWorkflowService().ListClosedWorkflowExecutions(request);
    assertTrue(listResponse.getExecutions().size() > 1);
    for (WorkflowExecutionInfo e : listResponse.getExecutions()) {
      assertEquals(WorkflowExecutionCloseStatus.CONTINUED_AS_NEW, e.getCloseStatus());
    }
  }

  @Test
  public void testMockedActivity() {
    GreetingActivities activities = mock(GreetingActivities.class);
    worker.registerActivitiesImplementations(activities);
    worker.start();

    // Get a workflow stub using the same task list the worker uses.
    GreetingWorkflow workflow = workflowClient.newWorkflowStub(GreetingWorkflow.class);
    // Execute a workflow waiting for it to complete.
    WorkflowExecution execution =
        WorkflowClient.start(workflow::greetPeriodically, "World", Duration.ofSeconds(1));
    assertEquals(PERIODIC_WORKFLOW_ID, execution.getWorkflowId());
    // Use TestWorkflowEnvironment.sleep to execute the unit test without really sleeping.
    testEnv.sleep(Duration.ofMinutes(1));
    verify(activities, atLeast(5)).greet(anyString());
  }
}
