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

package io.temporal.samples.polling.infrequent;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.polling.PollingWorkflow;
import io.temporal.samples.polling.TestService;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class InfrequentPollingStarter {
  private static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
  private static final WorkflowClient client = WorkflowClient.newInstance(service);
  private static final String taskQueue = "pollingSampleQueue";
  private static final String workflowId = "InfrequentPollingSampleWorkflow";

  public static void main(String[] args) {
    // Create our worker and register workflow and activities
    createWorker();

    // Create typed workflow stub and start execution (sync, wait for results)
    PollingWorkflow workflow =
        client.newWorkflowStub(
            PollingWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(taskQueue).setWorkflowId(workflowId).build());
    String result = workflow.exec();
    System.out.println("Result: " + result);
    System.exit(0);
  }

  private static void createWorker() {
    WorkerFactory workerFactory = WorkerFactory.newInstance(client);
    Worker worker = workerFactory.newWorker(taskQueue);

    // Register workflow and activities
    worker.registerWorkflowImplementationTypes(InfrequentPollingWorkflowImpl.class);
    worker.registerActivitiesImplementations(new InfrequentPollingActivityImpl(new TestService()));

    workerFactory.start();
  }
}
