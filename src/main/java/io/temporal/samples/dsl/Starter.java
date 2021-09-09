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

import static io.temporal.samples.dsl.DslWorkflowUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public class Starter {

  public static final WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
  public static final WorkflowClient client = WorkflowClient.newInstance(service);
  public static final WorkerFactory factory = WorkerFactory.newInstance(client);

  public static void main(String[] args) {
    try {
      // Read the workflow dsl
      Workflow dslWorkflow = Workflow.fromSource(getFileAsString("dsl/customerapplication.yml"));

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

      // Start workflow execution
      startWorkflow(workflowStub, dslWorkflow, getSampleWorkflowInput());

      // Wait for workflow to finish
      JsonNode result = workflowStub.getResult(JsonNode.class);
      // Print workflow results
      System.out.println("Workflow Results: \n" + result.toPrettyString());

    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }

    System.exit(0);
  }

  private static JsonNode getSampleWorkflowInput() throws Exception {
    String workflowDataInput = getFileAsString("dsl/datainput.json");
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readTree(workflowDataInput);
  }
}
