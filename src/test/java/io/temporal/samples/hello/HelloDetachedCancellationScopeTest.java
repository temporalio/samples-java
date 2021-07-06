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

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit test for {@link HelloDetachedCancellationScope}. Doesn't use an external Temporal service.
 */
public class HelloDetachedCancellationScopeTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloDetachedCancellationScope.GreetingWorkflowImpl.class)
          .setActivityImplementations(new HelloDetachedCancellationScope.GreetingActivitiesImpl())
          .build();

  @Test
  public void testDetachedWorkflowScope() {
    // Get a workflow stub using the same task queue the worker uses.
    HelloDetachedCancellationScope.GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloDetachedCancellationScope.GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

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
