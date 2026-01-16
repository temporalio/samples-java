package io.temporal.samples.temporalmetricsdemo;

import com.sun.net.httpserver.HttpServer;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.StatsReporter;
import com.uber.m3.util.Duration;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class TemporalConnection {
  private TemporalConnection() {}

  // Read from environment (docker-compose env_file: .env)
  public static final String NAMESPACE = env("TEMPORAL_NAMESPACE", "<namespace>.<account-id>");
  public static final String ADDRESS =
      env("TEMPORAL_ADDRESS", "<namespace>.<account-id>.tmprl.cloud:7233");
  public static final String CERT =
      env("TEMPORAL_CERT", "path/to/client.pem");
  public static final String KEY =
      env("TEMPORAL_KEY", "path/to/client.key");
  public static final String TASK_QUEUE = env("TASK_QUEUE", "openmetrics-task-queue");
  private static final int METRICS_PORT = envInt("METRICS_PORT", 9464);
  private static final int METRICS_REPORT_SECONDS = envInt("METRICS_REPORT_SECONDS", 10);

  private static volatile WorkflowClient CLIENT;
  private static volatile WorkflowServiceStubs SERVICE;

  private static volatile boolean METRICS_STARTED = false;
  private static volatile PrometheusMeterRegistry PROM_REGISTRY;

  public static WorkflowClient client() {
    if (CLIENT != null) return CLIENT;
    synchronized (TemporalConnection.class) {
      if (CLIENT != null) return CLIENT;

      SERVICE = serviceStubs();
      CLIENT =
          WorkflowClient.newInstance(
              SERVICE, WorkflowClientOptions.newBuilder().setNamespace(NAMESPACE).build());
      return CLIENT;
    }
  }

  // create service stubs used by worker + starter
  private static WorkflowServiceStubs serviceStubs() {
    try (InputStream clientCert = new FileInputStream(CERT);
        InputStream clientKey = new FileInputStream(KEY)) {

      SslContext sslContext =
          GrpcSslContexts.configure(SslContextBuilder.forClient().keyManager(clientCert, clientKey))
              .build();

      Scope metricsScope = metricsScope(); // ✅ tally scope that writes into Prometheus registry

      WorkflowServiceStubsOptions options =
          WorkflowServiceStubsOptions.newBuilder()
              .setTarget(ADDRESS)
              .setSslContext(sslContext)
              .setMetricsScope(metricsScope) // ✅ Temporal SDK emits metrics here
              .build();

      return WorkflowServiceStubs.newServiceStubs(options);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create Temporal TLS connection", e);
    }
  }

  private static Scope metricsScope() {
    synchronized (TemporalConnection.class) {
      if (PROM_REGISTRY == null) {
        PROM_REGISTRY = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
      }

      StatsReporter reporter = new MicrometerClientStatsReporter(PROM_REGISTRY);

      Scope scope =
          new RootScopeBuilder()
              .reporter(reporter)
              .reportEvery(Duration.ofSeconds(METRICS_REPORT_SECONDS));

      if (!METRICS_STARTED) {
        METRICS_STARTED = true;
        startMetricsHttpServer(PROM_REGISTRY);
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
      server.setExecutor(null);
      server.start();
      System.out.println("Worker metrics exposed at http://0.0.0.0:" + METRICS_PORT + "/metrics");
    } catch (Exception e) {
      throw new RuntimeException("Failed to start /metrics endpoint", e);
    }
  }

  private static String env(String key, String def) {
    String v = System.getenv(key);
    return (v == null || v.isBlank()) ? def : v;
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
