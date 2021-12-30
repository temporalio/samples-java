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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.serverlessworkflow.api.Workflow;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;

public class DslWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(DynamicDslWorkflow.class)
          .setActivityImplementations(new DslActivitiesImpl())
          .build();

  @Test
  public void testCustomerApplicationDSLWorkflow() throws Exception {
    Workflow dslWorkflow = DslWorkflowCache.getWorkflow("customerapplication", "1.0");

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();

    WorkflowStub workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newUntypedWorkflowStub(dslWorkflow.getName(), workflowOptions);

    workflow.start(
        dslWorkflow.getId(),
        dslWorkflow.getVersion(),
        getSampleWorkflowInput("dsl/customerapplication/datainput.json"));

    JsonNode result = workflow.getResult(JsonNode.class);

    assertNotNull(result);
    assertNotNull(result.get("customer"));
    assertNotNull(result.get("results"));
    ArrayNode results = (ArrayNode) result.get("results");
    assertNotNull(results);
    assertEquals(3, results.size());
    assertEquals("APPROVED", results.get(2).get("result").asText());
  }

  @Test
  public void testBankingTransactionsDSLWorkflow() throws Exception {
    Workflow dslWorkflow = DslWorkflowCache.getWorkflow("bankingtransactions", "1.0");

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();

    WorkflowStub workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newUntypedWorkflowStub(dslWorkflow.getName(), workflowOptions);

    workflow.start(
        dslWorkflow.getId(),
        dslWorkflow.getVersion(),
        getSampleWorkflowInput("dsl/bankingtransactions/datainput.json"));

    JsonNode result = workflow.getResult(JsonNode.class);

    assertNotNull(result);
    assertNotNull(result.get("customer"));
    assertNotNull(result.get("results"));
    ArrayNode results = (ArrayNode) result.get("results");
    assertNotNull(results);
    assertEquals(3, results.size());
    assertEquals("InvokeBankingService", results.get(0).get("type").asText());
    assertEquals("invoked", results.get(0).get("result").asText());
    assertEquals("InvokeBankingService", results.get(1).get("type").asText());
    assertEquals("invoked", results.get(1).get("result").asText());
    assertEquals("InvokeBankingService", results.get(2).get("type").asText());
    assertEquals("invoked", results.get(2).get("result").asText());
  }

  private static JsonNode getSampleWorkflowInput(String dataInputFileName) throws Exception {
    String workflowDataInput = getFileAsString(dataInputFileName);
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readTree(workflowDataInput);
  }

  private static String getFileAsString(String fileName) throws IOException {
    File file = new File(DslWorkflowTest.class.getClassLoader().getResource(fileName).getFile());
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }
}
