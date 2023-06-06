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

package io.temporal.samples.getresultsasync;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;

public class Worker {
  public static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
  public static final WorkflowClient client = WorkflowClient.newInstance(service);
  public static final WorkerFactory factory = WorkerFactory.newInstance(client);
  public static final String TASK_QUEUE_NAME = "asyncstartqueue";

  public static void main(String[] args) {
    io.temporal.worker.Worker worker = factory.newWorker(TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(MyWorkflowImpl.class);

    factory.start();
  }
}
