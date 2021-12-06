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

import io.temporal.client.WorkflowClient;
import io.temporal.samples.metrics.activities.MetricsActivitiesImpl;
import io.temporal.samples.metrics.workflow.MetricsWorkflowImpl;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class MetricsWorker {

  public static void main(String[] args) {

    // Add metrics scope to workflow service stub options
    WorkflowServiceStubsOptions stubOptions =
        WorkflowServiceStubsOptions.newBuilder().setMetricsScope(MetricsUtils.scope).build();

    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance(stubOptions);
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);

    // Start the prometheus scrape endpoint
    MetricsUtils.startPrometheusScrapeEndpoint();

    Worker worker = factory.newWorker(MetricsUtils.DEFAULT_TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(MetricsWorkflowImpl.class);
    worker.registerActivitiesImplementations(new MetricsActivitiesImpl());

    factory.start();

    // Stopping the worker will stop the http server that exposes the
    // scrape endpoint.
  }
}
