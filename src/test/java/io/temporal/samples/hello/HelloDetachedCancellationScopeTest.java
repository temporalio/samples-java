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

import static io.temporal.samples.hello.HelloDetachedCancellationScope.TASK_QUEUE;
import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link HelloDetachedCancellationScope}. Doesn't use an external Temporal service.
 */
public class HelloDetachedCancellationScopeTest {
  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  private WorkflowClient client;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(
        HelloDetachedCancellationScope.GreetingWorkflowImpl.class);

    client = testEnv.getWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testDetachedWorkflowScope() {
    worker.registerActivitiesImplementations(
        new HelloDetachedCancellationScope.GreetingActivitiesImpl());
    testEnv.start();

    // Get a workflow stub using the same task queue the worker uses.
    HelloDetachedCancellationScope.GreetingWorkflow workflow =
        client.newWorkflowStub(
            HelloDetachedCancellationScope.GreetingWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

    WorkflowClient.start(workflow::getGreeting, "John");

    WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);

    workflowStub.cancel();

    String result;

    try {
      // Wait for workflow results
      // Because we cancelled the workflow we should get WorkflowFailedException
      result = workflowStub.getResult(6, TimeUnit.SECONDS, String.class, String.class);
    } catch (TimeoutException | WorkflowFailedException e) {
      // Query the workflow to get the result which was set by the detached cancellation scope run
      result = workflowStub.query("queryGreeting", String.class);
    }
    assertEquals("Goodbye John!", result);
  }
}
