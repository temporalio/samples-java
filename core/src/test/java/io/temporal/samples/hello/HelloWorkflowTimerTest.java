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

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class HelloWorkflowTimerTest {
  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(
              HelloWorkflowTimer.WorkflowWithTimerImpl.class,
              HelloWorkflowTimer.WorkflowWithTimerChildWorkflowImpl.class)
          .setActivityImplementations(new HelloWorkflowTimer.WorkflowWithTimerActivitiesImpl())
          .build();

  @Test
  public void testWorkflowTimer() {
    HelloWorkflowTimer.WorkflowWithTimer workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloWorkflowTimer.WorkflowWithTimer.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("WorkflowWithTimerTestId")
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .build());

    String result = workflow.execute("test input");
    Assert.assertEquals("Workflow timer fired while activities were executing.", result);
  }
}
