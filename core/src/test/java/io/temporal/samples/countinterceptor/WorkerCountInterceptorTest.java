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

package io.temporal.samples.countinterceptor;

import static org.junit.Assert.*;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.samples.countinterceptor.activities.MyActivitiesImpl;
import io.temporal.samples.countinterceptor.workflow.MyChildWorkflowImpl;
import io.temporal.samples.countinterceptor.workflow.MyWorkflow;
import io.temporal.samples.countinterceptor.workflow.MyWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkerFactoryOptions;
import org.junit.Rule;
import org.junit.Test;

public class WorkerCountInterceptorTest {

  private static final String WORKFLOW_ID = "TestInterceptorWorkflow";
  private static final String CHILD_WORKFLOW_ID = "TestInterceptorChildWorkflow";

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(MyWorkflowImpl.class, MyChildWorkflowImpl.class)
          .setActivityImplementations(new MyActivitiesImpl())
          .setWorkerFactoryOptions(
              WorkerFactoryOptions.newBuilder()
                  .setWorkerInterceptors(new SimpleCountWorkerInterceptor())
                  .build())
          .build();

  @Test
  public void testInterceptor() {
    MyWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                MyWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .setWorkflowId(WORKFLOW_ID)
                    .build());

    WorkflowClient.start(workflow::exec);

    workflow.signalNameAndTitle("John", "Customer");

    String name = workflow.queryName();
    String title = workflow.queryTitle();

    workflow.exit();

    // Wait for workflow completion via WorkflowStub
    WorkflowStub untyped = WorkflowStub.fromTyped(workflow);
    String result = untyped.getResult(String.class);

    assertNotNull(result);

    assertNotNull(name);
    assertEquals("John", name);
    assertNotNull(title);
    assertEquals("Customer", title);

    assertEquals(1, WorkerCounter.getNumOfWorkflowExecutions(WORKFLOW_ID));
    assertEquals(1, WorkerCounter.getNumOfChildWorkflowExecutions(WORKFLOW_ID));
    // parent workflow does not execute any activities
    assertEquals(0, WorkerCounter.getNumOfActivityExecutions(WORKFLOW_ID));
    // child workflow executes 2 activities
    assertEquals(2, WorkerCounter.getNumOfActivityExecutions(CHILD_WORKFLOW_ID));
    assertEquals(2, WorkerCounter.getNumOfSignals(WORKFLOW_ID));
    assertEquals(2, WorkerCounter.getNumOfQueries(WORKFLOW_ID));
  }
}
