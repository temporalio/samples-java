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

import static io.temporal.samples.hello.HelloException.TASK_LIST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowException;
import io.temporal.client.WorkflowOptions;
import io.temporal.enums.v1.TimeoutType;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.failure.TimeoutFailure;
import io.temporal.samples.hello.HelloException.GreetingActivities;
import io.temporal.samples.hello.HelloException.GreetingChildImpl;
import io.temporal.samples.hello.HelloException.GreetingWorkflow;
import io.temporal.samples.hello.HelloException.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
  private WorkflowClient client;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(HelloException.TASK_LIST);

    client = testEnv.getWorkflowClient();
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
    testEnv.start();

    WorkflowOptions workflowOptions = WorkflowOptions.newBuilder().setTaskList(TASK_LIST).build();
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    try {
      workflow.getGreeting("World");
      throw new IllegalStateException("unreachable");
    } catch (WorkflowException e) {
      assertTrue(e.getCause() instanceof ChildWorkflowFailure);
      assertTrue(e.getCause().getCause() instanceof ActivityFailure);
      assertTrue(e.getCause().getCause().getCause() instanceof ApplicationFailure);
      assertEquals(
          "Hello World!",
          ((ApplicationFailure) e.getCause().getCause().getCause()).getOriginalMessage());
    }
  }

  @Test
  public void testActivityTimeout() {
    worker.registerWorkflowImplementationTypes(
        HelloException.GreetingWorkflowImpl.class, GreetingChildImpl.class);

    // Mock an activity that times out.
    GreetingActivities activities = mock(GreetingActivities.class);
    when(activities.composeGreeting(anyString(), anyString()))
        .thenThrow(
            new TimeoutFailure("simulated", null, TimeoutType.TIMEOUT_TYPE_SCHEDULE_TO_START));
    worker.registerActivitiesImplementations(activities);

    testEnv.start();

    WorkflowOptions workflowOptions = WorkflowOptions.newBuilder().setTaskList(TASK_LIST).build();
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    try {
      workflow.getGreeting("World");
      throw new IllegalStateException("unreachable");
    } catch (WorkflowException e) {
      assertTrue(e.getCause() instanceof ChildWorkflowFailure);
      Throwable doubleCause = e.getCause().getCause();
      assertTrue(doubleCause instanceof ActivityFailure);
      Throwable tripleCause = doubleCause.getCause();
      assertTrue(tripleCause instanceof TimeoutFailure);
      assertEquals(
          TimeoutType.TIMEOUT_TYPE_SCHEDULE_TO_START,
          ((TimeoutFailure) tripleCause).getTimeoutType());
    }
  }

  @Test(timeout = 100000)
  @Ignore // TODO(maxim): Find workaround for mockito breaking reflection
  public void testChildWorkflowTimeout() {
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    // Mock a child that times out.
    worker.addWorkflowImplementationFactory(
        HelloException.GreetingChild.class,
        () -> {
          GreetingChildImpl child = mock(GreetingChildImpl.class);
          when(child.composeGreeting(anyString(), anyString()))
              .thenThrow(
                  new TimeoutFailure("simulated", null, TimeoutType.TIMEOUT_TYPE_START_TO_CLOSE));
          return child;
        });

    testEnv.start();

    WorkflowOptions workflowOptions = WorkflowOptions.newBuilder().setTaskList(TASK_LIST).build();
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    try {
      workflow.getGreeting("World");
      throw new IllegalStateException("unreachable");
    } catch (WorkflowException e) {
      assertTrue(e.getCause() instanceof ChildWorkflowFailure);
      assertTrue(e.getCause().getCause() instanceof TimeoutFailure);
    }
  }
}
