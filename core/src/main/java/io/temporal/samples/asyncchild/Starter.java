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

package io.temporal.samples.asyncchild;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.util.concurrent.TimeUnit;

public class Starter {

  public static final String TASK_QUEUE = "asyncChildTaskQueue";
  private static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
  private static final WorkflowClient client = WorkflowClient.newInstance(service);
  private static final WorkerFactory factory = WorkerFactory.newInstance(client);

  public static void main(String[] args) {
    createWorker();

    WorkflowOptions parentWorkflowOptions =
        WorkflowOptions.newBuilder()
            .setWorkflowId("parentWorkflow")
            .setTaskQueue(TASK_QUEUE)
            .build();
    ParentWorkflow parentWorkflowStub =
        client.newWorkflowStub(ParentWorkflow.class, parentWorkflowOptions);

    // Start parent workflow and wait for it to complete
    WorkflowExecution childWorkflowExecution = parentWorkflowStub.executeParent();

    // Get the child workflow execution status (after parent completed)
    System.out.println("Child execution status: " + getStatusAsString(childWorkflowExecution));

    // Wait for child workflow to complete
    sleep(4);

    // Check the status of the child workflow again
    System.out.println("Child execution status: " + getStatusAsString(childWorkflowExecution));

    System.exit(0);
  }

  private static void createWorker() {
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(ParentWorkflowImpl.class, ChildWorkflowImpl.class);

    factory.start();
  }

  private static String getStatusAsString(WorkflowExecution execution) {
    DescribeWorkflowExecutionRequest describeWorkflowExecutionRequest =
        DescribeWorkflowExecutionRequest.newBuilder()
            .setNamespace(client.getOptions().getNamespace())
            .setExecution(execution)
            .build();

    DescribeWorkflowExecutionResponse resp =
        service.blockingStub().describeWorkflowExecution(describeWorkflowExecutionRequest);

    WorkflowExecutionInfo workflowExecutionInfo = resp.getWorkflowExecutionInfo();
    return workflowExecutionInfo.getStatus().toString();
  }

  private static void sleep(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (Exception e) {
      System.out.println("Exception: " + e.getMessage());
      System.exit(0);
    }
  }
}
