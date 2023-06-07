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

package io.temporal.samples.countinterceptor;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.interceptors.WorkflowClientInterceptor;
import io.temporal.samples.countinterceptor.activities.MyActivitiesImpl;
import io.temporal.samples.countinterceptor.workflow.MyChildWorkflowImpl;
import io.temporal.samples.countinterceptor.workflow.MyWorkflow;
import io.temporal.samples.countinterceptor.workflow.MyWorkflowImpl;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterceptorStarter {

  public static SimpleCountWorkerInterceptor workerInterceptor = new SimpleCountWorkerInterceptor();
  private static final String TEST_QUEUE = "test-queue";
  private static final String WORKFLOW_ID = "TestInterceptorWorkflow";

  private static final Logger logger = LoggerFactory.getLogger(SimpleCountWorkerInterceptor.class);

  public static void main(String[] args) {

    final ClientCounter clientCounter = new ClientCounter();
    final WorkflowClientInterceptor clientInterceptor = new SimpleClientInterceptor(clientCounter);

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setInterceptors(clientInterceptor).build());

    WorkerFactoryOptions wfo =
        WorkerFactoryOptions.newBuilder()
            .setWorkerInterceptors(workerInterceptor)
            .validateAndBuildWithDefaults();

    WorkerFactory factory = WorkerFactory.newInstance(client, wfo);

    Worker worker = factory.newWorker(TEST_QUEUE);
    worker.registerWorkflowImplementationTypes(MyWorkflowImpl.class, MyChildWorkflowImpl.class);
    worker.registerActivitiesImplementations(new MyActivitiesImpl());
    factory.start();

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setWorkflowId(WORKFLOW_ID).setTaskQueue(TEST_QUEUE).build();

    MyWorkflow workflow = client.newWorkflowStub(MyWorkflow.class, workflowOptions);

    WorkflowClient.start(workflow::exec);

    workflow.signalNameAndTitle("John", "Customer");

    String name = workflow.queryName();
    String title = workflow.queryTitle();

    // Send exit signal to workflow
    workflow.exit();

    // Wait for workflow completion via WorkflowStub
    WorkflowStub untyped = WorkflowStub.fromTyped(workflow);
    String result = untyped.getResult(String.class);

    // Print workflow
    logger.info("Workflow Result: " + result);

    // Print the Query results
    logger.info("Query results: ");
    logger.info("Name: " + name);
    logger.info("Title: " + title);

    // Print the Worker Counter Info
    logger.info("Collected Worker Counter Info: ");
    logger.info(WorkerCounter.getInfo());

    // Print the Client Counter Info
    logger.info("Collected Client Counter Info: ");
    logger.info(clientCounter.getInfo());

    System.exit(0);
  }
}
