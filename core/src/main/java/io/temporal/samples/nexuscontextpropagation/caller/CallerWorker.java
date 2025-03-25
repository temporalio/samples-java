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

package io.temporal.samples.nexuscontextpropagation.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.samples.nexuscontextpropagation.propagation.NexusMDCContextInterceptor;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;
import java.util.Collections;

public class CallerWorker {
  public static final String DEFAULT_TASK_QUEUE_NAME = "my-caller-workflow-task-queue";

  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient(args);

    WorkerFactory factory =
        WorkerFactory.newInstance(
            client,
            WorkerFactoryOptions.newBuilder()
                .setWorkerInterceptors(new NexusMDCContextInterceptor())
                .build());

    Worker worker = factory.newWorker(DEFAULT_TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(
        WorkflowImplementationOptions.newBuilder()
            .setNexusServiceOptions(
                Collections.singletonMap(
                    "NexusService",
                    NexusServiceOptions.newBuilder().setEndpoint("my-nexus-endpoint-name").build()))
            .build(),
        EchoCallerWorkflowImpl.class,
        HelloCallerWorkflowImpl.class);

    factory.start();
  }
}
