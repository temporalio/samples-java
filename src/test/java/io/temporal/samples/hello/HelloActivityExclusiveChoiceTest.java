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

import static io.temporal.samples.hello.HelloActivityExclusiveChoice.TASK_QUEUE;
import static io.temporal.samples.hello.HelloActivityExclusiveChoice.WORKFLOW_ID;
import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Unit test for {@link HelloActivityExclusiveChoice}. Doesn't use an external Temporal service. */
public class HelloActivityExclusiveChoiceTest {

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  private WorkflowClient client;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(
        HelloActivityExclusiveChoice.PurchaseFruitsWorkflowImpl.class);

    client = testEnv.getWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testWorkflow() {
    worker.registerActivitiesImplementations(
        new HelloActivityExclusiveChoice.OrderFruitsActivitiesImpl());
    testEnv.start();

    // Get a workflow stub using the same task queue the worker uses.
    HelloActivityExclusiveChoice.PurchaseFruitsWorkflow workflow =
        client.newWorkflowStub(
            HelloActivityExclusiveChoice.PurchaseFruitsWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());
    // Execute a workflow waiting for it to complete.
    HelloActivityExclusiveChoice.ShoppingList shoppingList =
        new HelloActivityExclusiveChoice.ShoppingList();
    shoppingList.addFruitOrder(HelloActivityExclusiveChoice.Fruits.APPLE, 10);
    StringBuilder orderResults = workflow.orderFruit(shoppingList);
    assertEquals("Ordered 10 Apples...", orderResults.toString());
  }
}
