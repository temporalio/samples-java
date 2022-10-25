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

import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Starter {
  public static final String TASK_QUEUE = "customerTaskQueue";
  private static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
  private static final WorkflowClient client = WorkflowClient.newInstance(service);
  private static final WorkerFactory factory = WorkerFactory.newInstance(client);

  public static void main(String[] args) {
    // create some fake customers
    List<Customer> customers = new ArrayList<>();
    customers.add(new Customer("c1", "John", "john@john.com", "new"));
    customers.add(new Customer("c2", "Mary", "mary@mary.com", "established"));
    customers.add(new Customer("c3", "Richard", "richard@richard.com", "established"));
    customers.add(new Customer("c4", "Anna", "anna@anna.com", "new"));
    customers.add(new Customer("c5", "Michael", "michael@michael.com", "established"));

    // create the worker for workflow and activities
    createWorker();

    // start customer workflows and define custom search attributes for each
    startWorkflows(customers);

    // small delay before we start querying executions
    try {
      Thread.sleep(2 * 1000);
    } catch (InterruptedException e) {
      throw new RuntimeException("Exception happened in thread sleep: ", e);
    }

    // query "new" customers for all "CustomerWorkflow" workflows with status "Running" (1)
    ListWorkflowExecutionsResponse newCustomersResponse =
        getExecutionsResponse(
            "WorkflowType='CustomerWorkflow' and CustomStringField='new' and ExecutionStatus="
                + WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING_VALUE);

    System.out.println("***** Customers with type 'new'");
    System.out.println(
        "Currently being processed: " + newCustomersResponse.getExecutionsList().size());
    List<WorkflowExecutionInfo> newExecutionInfo = newCustomersResponse.getExecutionsList();
    for (WorkflowExecutionInfo wei : newExecutionInfo) {
      System.out.println("Customer: " + wei.getExecution().getWorkflowId());
    }

    // query "established" customers for all "CustomerWorkflow" workflows with status "Running" (1)
    ListWorkflowExecutionsResponse establishedCustomersResponse =
        getExecutionsResponse(
            "WorkflowType = 'CustomerWorkflow' and CustomStringField='established' and ExecutionStatus="
                + WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING_VALUE);

    System.out.println("\n***** Customers with type 'established'");
    System.out.println(
        "Currently being processed: " + establishedCustomersResponse.getExecutionsList().size());
    List<WorkflowExecutionInfo> establishedExecutionInfo =
        establishedCustomersResponse.getExecutionsList();
    for (WorkflowExecutionInfo wei : establishedExecutionInfo) {
      System.out.println("Customer: " + wei.getExecution().getWorkflowId());
    }

    // signal exit to all customer workflows
    stopWorkflows(customers);

    // sleep for 3 seconds before we shut down the worker
    sleep(3);
    System.exit(0);
  }

  private static ListWorkflowExecutionsResponse getExecutionsResponse(String query) {
    ListWorkflowExecutionsRequest listWorkflowExecutionRequest =
        ListWorkflowExecutionsRequest.newBuilder()
            .setNamespace(client.getOptions().getNamespace())
            .setQuery(query)
            .build();
    ListWorkflowExecutionsResponse listWorkflowExecutionsResponse =
        service.blockingStub().listWorkflowExecutions(listWorkflowExecutionRequest);
    return listWorkflowExecutionsResponse;
  }

  private static void createWorker() {
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(CustomerWorkflowImpl.class);
    worker.registerActivitiesImplementations(new CustomerActivitiesImpl());

    factory.start();
  }

  private static Map<String, Object> generateSearchAttributesFor(Customer customer) {
    Map<String, Object> searchAttributes = new HashMap<>();
    searchAttributes.put("CustomStringField", customer.getCustomerType());

    return searchAttributes;
  }

  private static void startWorkflows(List<Customer> customers) {
    // start a workflow for each customer that we need to add message to account
    for (Customer c : customers) {
      String message = "New message for: " + c.getName();
      WorkflowOptions newCustomerWorkflowOptions =
          WorkflowOptions.newBuilder()
              .setWorkflowId(c.getAccountNum())
              .setTaskQueue(TASK_QUEUE)
              // set the search attributes for this customer workflow
              .setSearchAttributes(generateSearchAttributesFor(c))
              .build();
      CustomerWorkflow newCustomerWorkflow =
          client.newWorkflowStub(CustomerWorkflow.class, newCustomerWorkflowOptions);
      // start async
      WorkflowClient.start(newCustomerWorkflow::updateAccountMessage, c, message);
    }
  }

  private static void stopWorkflows(List<Customer> customers) {
    for (Customer c : customers) {
      CustomerWorkflow existingCustomerWorkflow =
          client.newWorkflowStub(CustomerWorkflow.class, c.getAccountNum());
      // signal the exist method to stop execution
      existingCustomerWorkflow.exit();
    }
  }

  private static void sleep(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException e) {
      System.out.println("Exception: " + e.getMessage());
      System.exit(0);
    }
  }
}
