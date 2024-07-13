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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.dsl.model.Flow;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class Starter {

  public static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
  public static final WorkflowClient client = WorkflowClient.newInstance(service);
  public static final WorkerFactory factory = WorkerFactory.newInstance(client);

  public static void main(String[] args) {
    Flow flow = getFlowFromResource();

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker("dsl-task-queue");
    worker.registerWorkflowImplementationTypes(DslWorkflowImpl.class);
    worker.registerActivitiesImplementations(new DslActivitiesImpl());
    factory.start();

    DslWorkflow workflow =
        client.newWorkflowStub(
            DslWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("dsl-workflow")
                .setTaskQueue("dsl-task-queue")
                .build());

    String result = workflow.run(flow, "sample input");

    System.out.println("Result: " + result);

    System.exit(0);
  }

  private static Flow getFlowFromResource() {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return objectMapper.readValue(
          Starter.class.getClassLoader().getResource("dsl/sampleflow.json"), Flow.class);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
