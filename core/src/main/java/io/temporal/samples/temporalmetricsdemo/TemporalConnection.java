package io.temporal.samples.temporalmetricsdemo;

import com.sun.net.httpserver.HttpServer;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.StatsReporter;
import com.uber.m3.util.Duration;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class TemporalConnection {
  private TemporalConnection() {}

  // Required: MUST be set in env. No defaults.
  public static final String NAMESPACE = envRequired("TEMPORAL_NAMESPACE");
  public static final String ADDRESS = envRequired("TEMPORAL_ADDRESS");
  public static final String TASK_QUEUE = env("TASK_QUEUE", "openmetrics-task-queue");

  private static final int METRICS_PORT = envInt("METRICS_PORT", 9464);
  private static final int METRICS_REPORT_SECONDS = envInt("METRICS_REPORT_SECONDS", 10);

  private static volatile WorkflowClient CLIENT;
  private static volatile PrometheusMeterRegistry PROM;
  private static volatile boolean METRICS_STARTED;

  public static WorkflowClient client() {
    if (CLIENT != null) return CLIENT;
    synchronized (TemporalConnection.class) {
      if (CLIENT != null) return CLIENT;

      String apiKey = envRequired("TEMPORAL_API_KEY");

      // Validation
      validate();
      System.out.println("TemporalConnection: ADDRESS=" + ADDRESS);
      System.out.println("TemporalConnection: NAMESPACE=" + NAMESPACE);

      Scope scope = metricsScope();

      WorkflowServiceStubs service =
          WorkflowServiceStubs.newServiceStubs(
              WorkflowServiceStubsOptions.newBuilder()
                  .setTarget(ADDRESS)
                  .setEnableHttps(true)
                  .addApiKey(() -> apiKey)
                  .setMetricsScope(scope)
                  .build());

      CLIENT =
          WorkflowClient.newInstance(
              service, WorkflowClientOptions.newBuilder().setNamespace(NAMESPACE).build());

      return CLIENT;
    }
  }

  private static void validate() {
    if (NAMESPACE.isBlank()) {
      throw new IllegalStateException("TEMPORAL_NAMESPACE must be set (non-blank).");
    }
    if (ADDRESS.isBlank()) {
      throw new IllegalStateException("TEMPORAL_ADDRESS must be set (non-blank).");
    }
  }

  private static Scope metricsScope() {
    synchronized (TemporalConnection.class) {
      if (PROM == null) PROM = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

      StatsReporter reporter = new MicrometerClientStatsReporter(PROM);
      Scope scope =
          new RootScopeBuilder()
              .reporter(reporter)
              .reportEvery(Duration.ofSeconds(METRICS_REPORT_SECONDS));

      if (!METRICS_STARTED) {
        METRICS_STARTED = true;
        startMetricsHttpServer(PROM);
      }
      return scope;
    }
  }

  private static void startMetricsHttpServer(PrometheusMeterRegistry registry) {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", METRICS_PORT), 0);
      server.createContext(
          "/metrics",
          exchange -> {
            byte[] body = registry.scrape().getBytes(StandardCharsets.UTF_8);
            exchange
                .getResponseHeaders()
                .add("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
              os.write(body);
            }
          });
      server.start();
      System.out.println("Worker metrics at http://0.0.0.0:" + METRICS_PORT + "/metrics");
    } catch (Exception e) {
      throw new RuntimeException("Failed to start /metrics endpoint", e);
    }
  }

  private static String env(String key, String def) {
    String v = System.getenv(key);
    return (v == null || v.isBlank()) ? def : v.trim();
  }

  private static String envRequired(String key) {
    String v = System.getenv(key);
    if (v == null || v.isBlank()) {
      throw new IllegalStateException("Missing required env var: " + key);
    }
    return v.trim();
  }

  private static int envInt(String key, int def) {
    String v = System.getenv(key);
    if (v == null || v.isBlank()) return def;
    try {
      return Integer.parseInt(v.trim());
    } catch (Exception e) {
      return def;
    }
  }
}
