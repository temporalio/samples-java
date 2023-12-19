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
import static org.junit.Assert.fail;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.hello.HelloAwait.GreetingWorkflow;
import io.temporal.testing.TestWorkflowRule;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloAwait}. Doesn't use an external Temporal service. */
public class HelloAwaitTest {

  private final String WORKFLOW_ID = "WORKFLOW1";

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder().setWorkflowTypes(HelloAwait.GreetingWorkflowImpl.class).build();

  @Test
  public void testAwaitSignal() {
    // Get a workflow stub using the same task queue the worker uses.
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowId(WORKFLOW_ID)
            .build();

    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Start workflow asynchronously to not use another thread to await.
    WorkflowClient.start(workflow::getGreeting);
    workflow.waitForName("World");
    // So we can send a await to it using workflow stub immediately.
    // But just to demonstrate the unit testing of a long running workflow adding a long sleep here.
    //    testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(30));

    WorkflowStub workflowById =
        testWorkflowRule.getWorkflowClient().newUntypedWorkflowStub(WORKFLOW_ID);

    String greeting = workflowById.getResult(String.class);
    assertEquals("Hello World!", greeting);
  }

  @Test
  public void testAwaitTimeout() {
    // Get a workflow stub using the same task queue the worker uses.
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowId(WORKFLOW_ID)
            .build();

    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Start workflow asynchronously to not use another thread to wait.
    WorkflowClient.start(workflow::getGreeting);

    // Skip time to force Await timeout
    testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(30));

    WorkflowStub workflowById =
        testWorkflowRule.getWorkflowClient().newUntypedWorkflowStub(WORKFLOW_ID);

    try {
      workflowById.getResult(String.class);
      fail("not reachable");
    } catch (WorkflowException e) {
      ApplicationFailure applicationFailure = (ApplicationFailure) e.getCause();
      assertEquals("signal-timeout", applicationFailure.getType());
    }
  }
}
