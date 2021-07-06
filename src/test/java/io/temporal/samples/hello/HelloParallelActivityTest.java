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

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloParallelActivity}. Doesn't use an external Temporal service. */
public class HelloParallelActivityTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloParallelActivity.MultiGreetingWorkflowImpl.class)
          .setActivityImplementations(new HelloParallelActivity.GreetingActivitiesImpl())
          .build();

  @Test
  public void testParallelActivity() {
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();

    HelloParallelActivity.MultiGreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(HelloParallelActivity.MultiGreetingWorkflow.class, workflowOptions);
    // Execute a workflow waiting for it to complete.
    List<String> results =
        workflow.getGreetings(Arrays.asList("John", "Marry", "Michael", "Janet"));
    assertEquals(4, results.size());
  }
}
