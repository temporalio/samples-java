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

package io.temporal.samples.listworkflows;

import static org.junit.Assert.assertEquals;

import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.ListOpenWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListOpenWorkflowExecutionsResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;

public class ListWorkflowsTest {
  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(CustomerWorkflowImpl.class)
          .setActivityImplementations(new CustomerActivitiesImpl())
          .build();

  @Test
  public void testActivityImpl() {
    // create some fake customers
    List<Customer> customers = new ArrayList<>();
    customers.add(new Customer("c1", "John", "john@john.com", "new"));
    customers.add(new Customer("c2", "Mary", "mary@mary.com", "established"));
    customers.add(new Customer("c3", "Richard", "richard@richard.com", "established"));
    customers.add(new Customer("c4", "Anna", "anna@anna.com", "new"));
    customers.add(new Customer("c5", "Michael", "michael@michael.com", "established"));

    startWorkflows(customers);

    List<WorkflowExecutionInfo> openExecutions = getExecutionsResponse();

    assertEquals(5, openExecutions.size());

    // signal exit to all customer workflows
    stopWorkflows(customers);
  }

  private void startWorkflows(List<Customer> customers) {
    // start a workflow for each customer that we need to add message to account
    for (Customer c : customers) {
      String message = "New message for: " + c.getName();
      WorkflowOptions newCustomerWorkflowOptions =
          WorkflowOptions.newBuilder()
              .setWorkflowId(c.getAccountNum())
              .setTaskQueue(testWorkflowRule.getTaskQueue())
              // set the search attributes for this customer workflow
              .setSearchAttributes(generateSearchAttributesFor(c))
              .build();
      CustomerWorkflow newCustomerWorkflow =
          testWorkflowRule
              .getWorkflowClient()
              .newWorkflowStub(CustomerWorkflow.class, newCustomerWorkflowOptions);
      // start async
      WorkflowClient.start(newCustomerWorkflow::updateAccountMessage, c, message);
    }
  }

  private Map<String, Object> generateSearchAttributesFor(Customer customer) {
    Map<String, Object> searchAttributes = new HashMap<>();
    searchAttributes.put("CustomStringField", customer.getCustomerType());

    return searchAttributes;
  }

  private List<WorkflowExecutionInfo> getExecutionsResponse() {
    // List open workflows and validate their types
    ListOpenWorkflowExecutionsRequest listRequest =
        ListOpenWorkflowExecutionsRequest.newBuilder()
            .setNamespace(testWorkflowRule.getTestEnvironment().getNamespace())
            .build();
    ListOpenWorkflowExecutionsResponse listResponse =
        testWorkflowRule
            .getTestEnvironment()
            .getWorkflowService()
            .blockingStub()
            .listOpenWorkflowExecutions(listRequest);
    return listResponse.getExecutionsList();
  }

  private void stopWorkflows(List<Customer> customers) {
    for (Customer c : customers) {
      CustomerWorkflow existingCustomerWorkflow =
          testWorkflowRule
              .getWorkflowClient()
              .newWorkflowStub(CustomerWorkflow.class, c.getAccountNum());
      // signal the exist method to stop execution
      existingCustomerWorkflow.exit();
    }
  }
}
