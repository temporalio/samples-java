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

package io.temporal.samples.getresultsasync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import java.util.concurrent.CompletableFuture;
import org.junit.Rule;
import org.junit.Test;

public class GetResultsAsyncTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder().setWorkflowTypes(MyWorkflowImpl.class).build();

  @Test
  public void testGetResultsAsync() throws Exception {

    WorkflowStub workflowStub =
        testWorkflowRule
            .getWorkflowClient()
            .newUntypedWorkflowStub(
                "MyWorkflow",
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    workflowStub.start(5);

    CompletableFuture<String> completableFuture = workflowStub.getResultAsync(String.class);

    String result = completableFuture.get();
    assertNotNull(result);
    assertEquals("woke up after 5 seconds", result);
  }
}
