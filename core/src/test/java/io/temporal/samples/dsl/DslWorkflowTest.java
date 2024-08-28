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

package io.temporal.samples.dsl;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.dsl.model.Flow;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class DslWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(DslWorkflowImpl.class)
          .setActivityImplementations(new DslActivitiesImpl())
          .build();

  @Test
  public void testDslWorkflow() throws Exception {
    DslWorkflow workflow =
        testWorkflowRule
            .getTestEnvironment()
            .getWorkflowClient()
            .newWorkflowStub(
                DslWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("dsl-workflow")
                    .setTaskQueue(testWorkflowRule.getWorker().getTaskQueue())
                    .build());

    String result = workflow.run(getFlowFromResource(), "test input");
    assertNotNull(result);
    assertEquals(
        "Activity one done...,Activity two done...,Activity three done...,Activity four done...",
        result);
  }

  private static Flow getFlowFromResource() {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return objectMapper.readValue(
          DslWorkflowTest.class.getClassLoader().getResource("dsl/sampleflow.json"), Flow.class);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
