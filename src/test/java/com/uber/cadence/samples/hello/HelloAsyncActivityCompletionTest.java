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

import com.uber.cadence.client.ActivityCompletionClient;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.samples.hello.HelloAsyncActivityCompletion.GreetingActivitiesImpl;
import com.uber.cadence.samples.hello.HelloAsyncActivityCompletion.GreetingWorkflow;
import com.uber.cadence.samples.hello.HelloAsyncActivityCompletion.GreetingWorkflowImpl;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;

/** Unit test for {@link HelloAsyncActivityCompletion}. Doesn't use an external Cadence service. */
public class HelloAsyncActivityCompletionTest {

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
    worker = testEnv.newWorker(HelloAsyncActivityCompletion.TASK_LIST);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    workflowClient = testEnv.newWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testActivityImpl() throws ExecutionException, InterruptedException {
    ActivityCompletionClient completionClient = workflowClient.newActivityCompletionClient();
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl(completionClient));
    worker.start();

    GreetingWorkflow workflow = workflowClient.newWorkflowStub(GreetingWorkflow.class);
    // Execute a workflow asynchronously.
    CompletableFuture<String> greeting = WorkflowClient.execute(workflow::getGreeting, "World");
    // Wait for workflow completion.
    assertEquals("Hello World!", greeting.get());
  }
}
