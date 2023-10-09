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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class MetricsUtils {

  /**
   * Starts HttpServer to expose a scrape endpoint. See
   * https://micrometer.io/docs/registry/prometheus for more info.
   */
  public static HttpServer startPrometheusScrapeEndpoint(
      PrometheusMeterRegistry registry, int port) {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
      server.createContext(
          "/metrics",
          httpExchange -> {
            String response = registry.scrape();
            httpExchange.sendResponseHeaders(200, response.getBytes(UTF_8).length);
            try (OutputStream os = httpExchange.getResponseBody()) {
              os.write(response.getBytes(UTF_8));
            }
          });

      server.start();
      return server;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
