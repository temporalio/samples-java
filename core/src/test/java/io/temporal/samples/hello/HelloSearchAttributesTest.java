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

import static io.temporal.samples.hello.HelloSearchAttributes.getKeywordFromSearchAttribute;

import io.temporal.api.common.v1.SearchAttributes;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import java.util.Map;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloSearchAttributes}. Doesn't use an external Temporal service. */
public class HelloSearchAttributesTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloSearchAttributes.GreetingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testStartWorkflowWithSearchAttribute() {

    final String taskQueue = testWorkflowRule.getTaskQueue();
    final String workflowId = "workflowId";
    final String customKeywordField = "CustomKeywordField";
    final String customKeywordValue = "CustomKeywordValue";

    testWorkflowRule
        .getWorker()
        .registerActivitiesImplementations(new HelloSearchAttributes.GreetingActivitiesImpl());
    testWorkflowRule.getTestEnvironment().start();

    // Get a workflow stub using the same task queue the worker uses.
    final HelloSearchAttributes.GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloSearchAttributes.GreetingWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setSearchAttributes(Map.of(customKeywordField, customKeywordValue))
                    .setWorkflowId(workflowId)
                    .setTaskQueue(taskQueue)
                    .build());

    final WorkflowStub untyped = WorkflowStub.fromTyped(workflow);
    final WorkflowExecution execution = untyped.start("Hello");

    // wait until workflow completes
    untyped.getResult(String.class);

    final DescribeWorkflowExecutionResponse resp =
        testWorkflowRule
            .getWorkflowClient()
            .getWorkflowServiceStubs()
            .blockingStub()
            .describeWorkflowExecution(
                DescribeWorkflowExecutionRequest.newBuilder()
                    .setNamespace(testWorkflowRule.getTestEnvironment().getNamespace())
                    .setExecution(execution)
                    .build());
    // get all search attributes
    final SearchAttributes searchAttributes = resp.getWorkflowExecutionInfo().getSearchAttributes();

    Assert.assertEquals(
        customKeywordValue, getKeywordFromSearchAttribute(searchAttributes, customKeywordField));
  }
}
