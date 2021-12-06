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

package io.temporal.samples.metrics;

import static io.temporal.samples.metrics.MetricsWorker.DEFAULT_TASK_QUEUE_NAME;
import static io.temporal.samples.metrics.MetricsWorker.client;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.metrics.workflow.MetricsWorkflow;

public class MetricsStarter {
  public static void main(String[] args) {
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setWorkflowId("metricsWorkflow")
            .setTaskQueue(DEFAULT_TASK_QUEUE_NAME)
            .build();
    MetricsWorkflow workflow = client.newWorkflowStub(MetricsWorkflow.class, workflowOptions);

    String result = workflow.exec("hello metrics");

    System.out.println("Result: " + result);

    System.out.println("Check metrics at http://localhost:8080/sdkmetrics");
  }
}
