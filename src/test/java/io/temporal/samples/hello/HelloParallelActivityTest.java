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
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Unit test for {@link HelloParallelActivity}. Doesn't use an external Temporal service. */
public class HelloParallelActivityTest {

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  private WorkflowClient client;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(HelloParallelActivity.TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(
        HelloParallelActivity.MultiGreetingWorkflowImpl.class);
    worker.registerActivitiesImplementations(new HelloParallelActivity.GreetingActivitiesImpl());

    client = testEnv.getWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testParallelActivity() {
    testEnv.start();

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(HelloParallelActivity.TASK_QUEUE).build();

    HelloParallelActivity.MultiGreetingWorkflow workflow =
        client.newWorkflowStub(HelloParallelActivity.MultiGreetingWorkflow.class, workflowOptions);
    // Execute a workflow waiting for it to complete.
    List<String> results = workflow.getGreetings("John", "Marry", "Michael", "Janet");
    assertEquals(4, results.size());
  }
}
