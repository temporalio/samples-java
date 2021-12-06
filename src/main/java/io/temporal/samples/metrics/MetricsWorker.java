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
import com.uber.m3.tally.StatsReporter;
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
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class MetricsWorker {

  // Set up prometheus registry and stats reported
  private static final PrometheusMeterRegistry registry =
      new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  public static final StatsReporter reporter = new MicrometerClientStatsReporter(registry);
  // Set up a new scope, report every 10s
  private static final Scope scope =
      new RootScopeBuilder().reporter(reporter).reportEvery(com.uber.m3.util.Duration.ofSeconds(1));

  private static final WorkflowServiceStubsOptions stubOptions =
      WorkflowServiceStubsOptions.newBuilder().setMetricsScope(scope).build();
  private static final WorkflowServiceStubs service = WorkflowServiceStubs.newInstance(stubOptions);

  public static final WorkflowClient client = WorkflowClient.newInstance(service);
  private static final WorkerFactory factory = WorkerFactory.newInstance(client);
  public static final String DEFAULT_TASK_QUEUE_NAME = "metricsqueue";

  public static void main(String[] args) {

    startServer();

    Worker worker = factory.newWorker(DEFAULT_TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(MetricsWorkflowImpl.class);
    worker.registerActivitiesImplementations(new MetricsActivitiesImpl());

    factory.start();

    // Stopping the worker will stop the http server that exposes the
    // scrape endpoint.
  }

  /**
   * Starts HttpServer to expose a scrape endpoint. See
   * https://micrometer.io/docs/registry/prometheus for more info.
   */
  private static void startServer() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
      server.createContext(
          "/sdkmetrics",
          httpExchange -> {
            String response = registry.scrape();
            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = httpExchange.getResponseBody()) {
              os.write(response.getBytes());
            }
          });

      new Thread(server::start).start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
