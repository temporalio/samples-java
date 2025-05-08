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

package io.temporal.samples.autoheartbeat;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.autoheartbeat.activities.AutoActivitiesImpl;
import io.temporal.samples.autoheartbeat.interceptor.AutoHeartbeatWorkerInterceptor;
import io.temporal.samples.autoheartbeat.workflows.AutoWorkflow;
import io.temporal.samples.autoheartbeat.workflows.AutoWorkflowImpl;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;

public class Starter {
  static final String TASK_QUEUE = "AutoheartbeatTaskQueue";
  static final String WORKFLOW_ID = "AutoHeartbeatWorkflow";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    // Configure our auto heartbeat workflow interceptor which will apply
    // AutoHeartbeaterUtil to each activity workflow schedules which has a heartbeat
    // timeout configured
    WorkerFactoryOptions wfo =
        WorkerFactoryOptions.newBuilder()
            .setWorkerInterceptors(new AutoHeartbeatWorkerInterceptor())
            .build();

    WorkerFactory factory = WorkerFactory.newInstance(client, wfo);
    Worker worker = factory.newWorker(TASK_QUEUE);

    worker.registerWorkflowImplementationTypes(AutoWorkflowImpl.class);
    worker.registerActivitiesImplementations(new AutoActivitiesImpl());

    factory.start();

    AutoWorkflow workflow =
        client.newWorkflowStub(
            AutoWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    try {
      String result = workflow.exec("Auto heartbeating is cool");
      System.out.println("Result: " + result);
    } catch (Exception e) {
      System.out.println("Workflow exec exception: " + e.getClass().getName());
    }
  }
}
