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

package io.temporal.samples.interceptor;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.interceptor.activities.MyActivitiesImpl;
import io.temporal.samples.interceptor.workflow.MyChildWorkflowImpl;
import io.temporal.samples.interceptor.workflow.MyWorkflow;
import io.temporal.samples.interceptor.workflow.MyWorkflowImpl;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;

public class InterceptorStarter {

  public static SimpleCountWorkerInterceptor interceptor = new SimpleCountWorkerInterceptor();

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactoryOptions wfo =
        WorkerFactoryOptions.newBuilder()
            .setWorkerInterceptors(interceptor)
            .validateAndBuildWithDefaults();

    WorkerFactory factory = WorkerFactory.newInstance(client, wfo);

    Worker worker = factory.newWorker("test-queue");
    worker.registerWorkflowImplementationTypes(MyWorkflowImpl.class, MyChildWorkflowImpl.class);
    worker.registerActivitiesImplementations(new MyActivitiesImpl());
    factory.start();

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setWorkflowId("TestWorkflow")
            .setTaskQueue("test-queue")
            .build();

    MyWorkflow workflow = client.newWorkflowStub(MyWorkflow.class, workflowOptions);

    WorkflowClient.start(workflow::exec);

    workflow.signalNameAndTitle("John", "Customer");

    String name = workflow.queryName();
    String title = workflow.queryTitle();

    System.out.println("Name: " + name);
    System.out.println("Title: " + title);

    try {
      Thread.sleep(2500);
      System.out.println(interceptor.getCountCollector().toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.exit(0);
  }
}
