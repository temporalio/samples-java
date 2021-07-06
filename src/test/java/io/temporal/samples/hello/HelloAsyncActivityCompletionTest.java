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

import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloAsyncActivityCompletion.GreetingActivitiesImpl;
import io.temporal.samples.hello.HelloAsyncActivityCompletion.GreetingWorkflow;
import io.temporal.samples.hello.HelloAsyncActivityCompletion.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloAsyncActivityCompletion}. Doesn't use an external Temporal service. */
public class HelloAsyncActivityCompletionTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(GreetingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testActivityImpl() throws ExecutionException, InterruptedException {
    ActivityCompletionClient completionClient =
        testWorkflowRule.getWorkflowClient().newActivityCompletionClient();
    testWorkflowRule
        .getWorker()
        .registerActivitiesImplementations(new GreetingActivitiesImpl(completionClient));
    testWorkflowRule.getTestEnvironment().start();

    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    // Execute a workflow asynchronously.
    CompletableFuture<String> greeting = WorkflowClient.execute(workflow::getGreeting, "World");
    // Wait for workflow completion.
    assertEquals("Hello World!", greeting.get());

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
