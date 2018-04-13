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

import static com.uber.cadence.samples.hello.HelloException.TASK_LIST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.uber.cadence.TimeoutType;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowException;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.samples.hello.HelloException.GreetingActivities;
import com.uber.cadence.samples.hello.HelloException.GreetingChildImpl;
import com.uber.cadence.samples.hello.HelloException.GreetingWorkflow;
import com.uber.cadence.samples.hello.HelloException.GreetingWorkflowImpl;
import com.uber.cadence.testing.SimulatedTimeoutException;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.ActivityFailureException;
import com.uber.cadence.workflow.ActivityTimeoutException;
import com.uber.cadence.workflow.ChildWorkflowFailureException;
import com.uber.cadence.workflow.ChildWorkflowTimedOutException;
import java.io.IOException;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class HelloExceptionTest {

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
    worker = testEnv.newWorker(HelloException.TASK_LIST);

    workflowClient = testEnv.newWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testIOException() {
    worker.registerWorkflowImplementationTypes(
        HelloException.GreetingWorkflowImpl.class, GreetingChildImpl.class);
    worker.registerActivitiesImplementations(new HelloException.GreetingActivitiesImpl());
    worker.start();

    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(TASK_LIST)
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    GreetingWorkflow workflow =
        workflowClient.newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    try {
      workflow.getGreeting("World");
      throw new IllegalStateException("unreachable");
    } catch (WorkflowException e) {
      assertTrue(e.getCause() instanceof ChildWorkflowFailureException);
      assertTrue(e.getCause().getCause() instanceof ActivityFailureException);
      assertTrue(e.getCause().getCause().getCause() instanceof IOException);
      assertEquals("Hello World!", e.getCause().getCause().getCause().getMessage());
    }
  }

  @Test
  public void testActivityTimeout() {
    worker.registerWorkflowImplementationTypes(
        HelloException.GreetingWorkflowImpl.class, GreetingChildImpl.class);

    // Mock an activity that times out.
    GreetingActivities activities = mock(GreetingActivities.class);
    when(activities.composeGreeting(anyString(), anyString()))
        .thenThrow(new SimulatedTimeoutException(TimeoutType.SCHEDULE_TO_START));
    worker.registerActivitiesImplementations(activities);

    worker.start();

    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(TASK_LIST)
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    GreetingWorkflow workflow =
        workflowClient.newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    try {
      workflow.getGreeting("World");
      throw new IllegalStateException("unreachable");
    } catch (WorkflowException e) {
      assertTrue(e.getCause() instanceof ChildWorkflowFailureException);
      Throwable doubleCause = e.getCause().getCause();
      assertTrue(doubleCause instanceof ActivityTimeoutException);
      ActivityTimeoutException timeoutException = (ActivityTimeoutException) doubleCause;
      assertEquals(TimeoutType.SCHEDULE_TO_START, timeoutException.getTimeoutType());
    }
  }

  @Test(timeout = 1000)
  public void testChildWorkflowTimeout() {
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    // Mock a child that times out.
    worker.addWorkflowImplementationFactory(
        GreetingChildImpl.class,
        () -> {
          GreetingChildImpl child = mock(GreetingChildImpl.class);
          when(child.composeGreeting(anyString(), anyString()))
              .thenThrow(new SimulatedTimeoutException());
          return child;
        });

    worker.start();

    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(TASK_LIST)
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    GreetingWorkflow workflow =
        workflowClient.newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    try {
      workflow.getGreeting("World");
      throw new IllegalStateException("unreachable");
    } catch (WorkflowException e) {
      assertTrue(e.getCause() instanceof ChildWorkflowTimedOutException);
    }
  }
}
