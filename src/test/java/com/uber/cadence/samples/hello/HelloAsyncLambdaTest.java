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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.samples.hello.HelloAsyncLambda.GreetingActivities;
import com.uber.cadence.samples.hello.HelloAsyncLambda.GreetingActivitiesImpl;
import com.uber.cadence.samples.hello.HelloAsyncLambda.GreetingWorkflow;
import com.uber.cadence.samples.hello.HelloAsyncLambda.GreetingWorkflowImpl;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;

/** Unit test for {@link HelloAsyncLambda}. Doesn't use an external Cadence service. */
public class HelloAsyncLambdaTest {

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
    worker = testEnv.newWorker(HelloAsyncLambda.TASK_LIST);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    workflowClient = testEnv.newWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testActivityImpl() {
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
    worker.start();

    // Get a workflow stub using the same task list the worker uses.
    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(HelloAsyncLambda.TASK_LIST)
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    GreetingWorkflow workflow =
        workflowClient.newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!\nHello World!", greeting);
  }

  @Test
  public void testMockedActivity() {
    GreetingActivities activities = mock(GreetingActivities.class);
    when(activities.getGreeting()).thenReturn("Hello");
    when(activities.composeGreeting("Hello", "World")).thenReturn("Hello World!");
    worker.registerActivitiesImplementations(activities);
    worker.start();

    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(HelloAsyncLambda.TASK_LIST)
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    GreetingWorkflow workflow =
        workflowClient.newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!\nHello World!", greeting);

    verify(activities, times(2)).composeGreeting(anyString(), anyString());
    verify(activities, times(2)).getGreeting();
  }
}
