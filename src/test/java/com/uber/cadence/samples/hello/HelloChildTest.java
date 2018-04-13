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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.samples.hello.HelloChild.GreetingChild;
import com.uber.cadence.samples.hello.HelloChild.GreetingChildImpl;
import com.uber.cadence.samples.hello.HelloChild.GreetingWorkflow;
import com.uber.cadence.samples.hello.HelloChild.GreetingWorkflowImpl;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/** Unit test for {@link HelloChild}. Doesn't use an external Cadence service. */
public class HelloChildTest {

  /** Prints workflow histories under test in case of a test failure. */
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
    worker = testEnv.newWorker(HelloChild.TASK_LIST);

    workflowClient = testEnv.newWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testChild() {
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class, GreetingChildImpl.class);

    worker.start();

    // Get a workflow stub using the same task list the worker uses.
    GreetingWorkflow workflow = workflowClient.newWorkflowStub(GreetingWorkflow.class);
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!", greeting);
  }

  @Test
  public void testMockedChild() {
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    // As new mock is created on each decision the only last one is useful to verify calls.
    AtomicReference<GreetingChild> lastChildMock = new AtomicReference<>();
    // Factory is called to create a new workflow object on each decision.
    worker.addWorkflowImplementationFactory(
        GreetingChild.class,
        () -> {
          GreetingChild child = mock(GreetingChild.class);
          when(child.composeGreeting("Hello", "World")).thenReturn("Hello World!");
          lastChildMock.set(child);
          return child;
        });
    worker.start();

    // Get a workflow stub using the same task list the worker uses.
    GreetingWorkflow workflow = workflowClient.newWorkflowStub(GreetingWorkflow.class);
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!", greeting);
    GreetingChild mock = lastChildMock.get();
    verify(mock).composeGreeting(eq("Hello"), eq("World"));
  }
}
