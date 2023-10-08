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

import com.sun.net.httpserver.HttpServer;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.ImmutableMap;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.temporal.client.WorkflowClient;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.samples.metrics.activities.MetricsActivitiesImpl;
import io.temporal.samples.metrics.workflow.MetricsWorkflowImpl;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class MetricsWorker {

  // task queue to be used for this sample
  public static final String DEFAULT_TASK_QUEUE_NAME = "metricsqueue";

  public static void main(String[] args) {

    // Set up prometheus registry and stats reported
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    // Set up a new scope, report every 1 second
    Scope scope =
        new RootScopeBuilder()
            // shows how to set custom tags
            .tags(
                ImmutableMap.of(
                    "workerCustomTag1",
                    "workerCustomTag1Value",
                    "workerCustomTag2",
                    "workerCustomTag2Value"))
            .reporter(new MicrometerClientStatsReporter(registry))
            .reportEvery(com.uber.m3.util.Duration.ofSeconds(1));
    // Start the prometheus scrape endpoint
    HttpServer scrapeEndpoint = MetricsUtils.startPrometheusScrapeEndpoint(registry, 8077);
    // Stopping the worker will stop the http server that exposes the
    // scrape endpoint.
    Runtime.getRuntime().addShutdownHook(new Thread(() -> scrapeEndpoint.stop(1)));
    // Add metrics scope to workflow service stub options
    WorkflowServiceStubsOptions stubOptions =
        WorkflowServiceStubsOptions.newBuilder().setMetricsScope(scope).build();

    WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(stubOptions);
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(DEFAULT_TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(MetricsWorkflowImpl.class);
    worker.registerActivitiesImplementations(new MetricsActivitiesImpl());

    factory.start();

    System.out.println("Workers metrics are available at http://localhost:8080/prometheus");
  }
}
