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

package io.temporal.samples.peractivityoptions;

import com.google.common.collect.ImmutableMap;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import java.time.Duration;

public class Starter {

  public static final String TASK_QUEUE = "perActivityTaskQueue";
  private static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
  private static final WorkflowClient client = WorkflowClient.newInstance(service);
  private static final WorkerFactory factory = WorkerFactory.newInstance(client);

  public static void main(String[] args) {
    // Create Worker
    Worker worker = factory.newWorker(TASK_QUEUE);
    // Register workflow impl and set the per-activity options
    WorkflowImplementationOptions options =
        WorkflowImplementationOptions.newBuilder()
            // setActivityOptions allows you to set different ActivityOption per activity type
            // By default activity type is the name of activity method (with first letter upper
            // cased)
            .setActivityOptions(
                ImmutableMap.of(
                    "ActivityTypeA",
                    ActivityOptions.newBuilder()
                        // Set activity exec timeout (including retries)
                        .setScheduleToCloseTimeout(Duration.ofSeconds(5))
                        // Set activity type specific retries if needed
                        .build(),
                    "ActivityTypeB",
                    ActivityOptions.newBuilder()
                        // Set activity exec timeout (single run)
                        .setStartToCloseTimeout(Duration.ofSeconds(2))
                        .setRetryOptions(
                            RetryOptions.newBuilder()
                                // ActivityTypeB activity type shouldn't retry on NPE
                                .setDoNotRetry(NullPointerException.class.getName())
                                .build())
                        .build()))
            .build();

    // Register our workflow impl and give the per-activity options
    // Note you can register multiple workflow impls with worker using these activity options
    worker.registerWorkflowImplementationTypes(options, PerActivityOptionsWorkflowImpl.class);

    // Register activity impl with worker
    worker.registerActivitiesImplementations(new FailingActivitiesImpl());

    factory.start();

    // Create typed workflow stub
    PerActivityOptionsWorkflow workflow =
        client.newWorkflowStub(
            PerActivityOptionsWorkflow.class,
            WorkflowOptions.newBuilder()
                // set business level id
                .setWorkflowId("PerActivityOptionsWorkflow")
                // set same task queue as our worker
                .setTaskQueue(TASK_QUEUE)
                .build());

    // Call our workflow method sync (wait for results)
    workflow.execute();

    System.exit(0);
  }
}
