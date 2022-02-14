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

import static io.temporal.samples.dsl.utils.DslWorkflowUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.interfaces.WorkflowValidator;
import io.serverlessworkflow.api.validation.ValidationError;
import io.serverlessworkflow.validation.WorkflowValidatorImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import java.util.List;
import java.util.Map;

public class Starter {

  public static final WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
  public static final WorkflowClient client = WorkflowClient.newInstance(service);
  public static final WorkerFactory factory = WorkerFactory.newInstance(client);

  private static final Map<String, String> workflowIdToDataInputMap =
      ImmutableMap.of(
          "customerapplication",
          "dsl/customerapplication/datainput.json",
          "bankingtransactions",
          "dsl/bankingtransactions/datainput.json");

  public static void main(String[] args) {
    for (String workflowId : workflowIdToDataInputMap.keySet()) {
      dslWorkflow(workflowId, "1.0", workflowIdToDataInputMap.get(workflowId));
    }

    System.exit(0);
  }

  private static void dslWorkflow(
      String workflowId, String workflowVersion, String dataInputFileName) {
    try {
      // Get the workflow dsl from cache
      Workflow dslWorkflow = DslWorkflowCache.getWorkflow(workflowId, workflowVersion);

      // Validate dsl
      WorkflowValidator dslWorkflowValidator = new WorkflowValidatorImpl();
      if (!dslWorkflowValidator.setWorkflow(dslWorkflow).isValid()) {
        System.err.println(
            "Workflow DSL not valid. Consult github.com/serverlessworkflow/specification/blob/main/specification.md for more info");
        List<ValidationError> validationErrorList =
            dslWorkflowValidator.setWorkflow(dslWorkflow).validate();
        for (ValidationError error : validationErrorList) {
          System.out.println("Error: " + error.getMessage());
        }
        System.exit(1);
      }

      WorkflowOptions workflowOptions = getWorkflowOptions(dslWorkflow);

      WorkflowStub workflowStub =
          client.newUntypedWorkflowStub(dslWorkflow.getName(), workflowOptions);

      System.out.println(
          "Starting workflow with id: " + workflowId + " and version: " + workflowVersion);
      // Start workflow execution
      startWorkflow(workflowStub, dslWorkflow, getSampleWorkflowInput(dataInputFileName));

      // Wait for workflow to finish
      JsonNode result = workflowStub.getResult(JsonNode.class);

      // Query the customer name and age
      String customerName = workflowStub.query("QueryCustomerName", String.class);
      int customerAge = workflowStub.query("QueryCustomerAge", Integer.class);

      System.out.println("Query result for customer name: " + customerName);
      System.out.println("Query result for customer age: " + customerAge);

      // Print workflow results
      System.out.println("Workflow results: \n" + result.toPrettyString());

    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Error: " + e.getMessage());
    }
  }
}
