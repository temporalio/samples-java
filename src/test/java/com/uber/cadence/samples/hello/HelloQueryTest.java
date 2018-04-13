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

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.samples.hello.HelloQuery.GreetingWorkflow;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/** Unit test for {@link HelloQuery}. Doesn't use an external Cadence service. */
public class HelloQueryTest {

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

    worker = testEnv.newWorker(HelloQuery.TASK_LIST);
    // Comment the above line and uncomment the below one to see how the TestWatcher rule prints
    // the history of the stuck workflow as its decision task is never picked up.
    // worker = testEnv.newWorker("InvalidTaskList");

    worker.registerWorkflowImplementationTypes(HelloQuery.GreetingWorkflowImpl.class);
    worker.start();

    workflowClient = testEnv.newWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test(timeout = 5000)
  public void testQuery() {
    // Get a workflow stub using the same task list the worker uses.
    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(HelloQuery.TASK_LIST)
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    GreetingWorkflow workflow =
        workflowClient.newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Start workflow asynchronously to not use another thread to query.
    WorkflowClient.start(workflow::createGreeting, "World");

    // After start for getGreeting returns, the workflow is guaranteed to be started.
    // So we can send a signal to it using workflow stub.
    assertEquals("Hello World!", workflow.queryGreeting());

    // Unit tests should call TestWorkflowEnvironment.sleep.
    // It allows skipping the time if workflow is just waiting on a timer
    // and executing tests of long running workflows very fast.
    // Note that this unit test executes under a second and not
    // over 3 as it would if Thread.sleep(3000) was called.
    testEnv.sleep(Duration.ofSeconds(3));

    assertEquals("Bye World!", workflow.queryGreeting());
  }
}
