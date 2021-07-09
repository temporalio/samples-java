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

package io.temporal.samples.contextpropagation;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.contextpropagation.activities.PropActivitiesImpl;
import io.temporal.samples.contextpropagation.workflow.PropChildWorkflowImpl;
import io.temporal.samples.contextpropagation.workflow.PropWorkflow;
import io.temporal.samples.contextpropagation.workflow.PropWorkflowImpl;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.util.Collections;

public class Starter {

  public static final String TASK_QUEUE = "contextPropTaskQueue";
  public static final String WORKFLOW_ID = "contextPropWorkflow";
  private static final WorkflowClientOptions clientOptions =
      WorkflowClientOptions.newBuilder()
          .setContextPropagators(Collections.singletonList(new PropertiesContextPropagator()))
          .build();

  private static final WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
  private static final WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);
  private static final WorkerFactory factory = WorkerFactory.newInstance(client);

  public static void main(String[] args) {

    // create the worker for workflow and activities
    createWorker();

    WorkflowOptions options =
        WorkflowOptions.newBuilder().setWorkflowId(WORKFLOW_ID).setTaskQueue(TASK_QUEUE).build();

    PropWorkflow propWorkflow = client.newWorkflowStub(PropWorkflow.class, options);

    String result = propWorkflow.exec();

    System.out.println(result);
    System.exit(0);
  }

  private static void createWorker() {
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(PropWorkflowImpl.class, PropChildWorkflowImpl.class);
    worker.registerActivitiesImplementations(new PropActivitiesImpl());

    factory.start();
  }
}
