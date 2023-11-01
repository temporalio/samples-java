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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.temporal.api.enums.v1.TimeoutType;
import io.temporal.client.WorkflowException;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.failure.TimeoutFailure;
import io.temporal.samples.hello.HelloException.GreetingChildImpl;
import io.temporal.samples.hello.HelloException.GreetingWorkflow;
import io.temporal.samples.hello.HelloException.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class HelloExceptionTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder().setDoNotStart(true).build();

  @Test
  public void testIOException() {
    testWorkflowRule
        .getWorker()
        .registerWorkflowImplementationTypes(
            HelloException.GreetingWorkflowImpl.class, GreetingChildImpl.class);
    testWorkflowRule
        .getWorker()
        .registerActivitiesImplementations(new HelloException.GreetingActivitiesImpl());
    testWorkflowRule.getTestEnvironment().start();

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(GreetingWorkflow.class, workflowOptions);

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

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test
  public void testActivityScheduleToStartTimeout() {
    testWorkflowRule
        .getWorker()
        .registerWorkflowImplementationTypes(
            HelloException.GreetingWorkflowImpl.class, GreetingChildImpl.class);

    // We don't register an activity implementation on the worker and the activity has 5 seconds
    // schedule to start timeout in GreetingChildImpl

    testWorkflowRule.getTestEnvironment().start();

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(GreetingWorkflow.class, workflowOptions);
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

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test(timeout = 100000)
  public void testChildWorkflowTimeout() {
    testWorkflowRule.getWorker().registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    // Mock a child that times out.
    testWorkflowRule
        .getWorker()
        .registerWorkflowImplementationFactory(
            HelloException.GreetingChild.class,
            () -> {
              GreetingChildImpl child = mock(GreetingChildImpl.class);
              when(child.composeGreeting(anyString(), anyString()))
                  .thenThrow(
                      new TimeoutFailure(
                          "simulated", null, TimeoutType.TIMEOUT_TYPE_START_TO_CLOSE));
              return child;
            });

    testWorkflowRule.getTestEnvironment().start();

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    try {
      workflow.getGreeting("World");
      throw new IllegalStateException("unreachable");
    } catch (WorkflowException e) {
      assertTrue(e.getCause() instanceof ChildWorkflowFailure);
      assertTrue(e.getCause().getCause() instanceof TimeoutFailure);
    }

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
