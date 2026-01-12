package io.temporal.samples.temporalcloudopenmetrics;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.io.FileInputStream;
import java.io.InputStream;

public final class TemporalConnection {
  private TemporalConnection() {}

  // add your namespace here
  public static final String NAMESPACE = env("TEMPORAL_NAMESPACE", "deepika-test-namespace.a2dd6");

  public static final String ADDRESS =
      env("TEMPORAL_ADDRESS", "deepika-test-namespace.a2dd6.tmprl.cloud:7233");

  public static final String CERT =
      env("TEMPORAL_CERT", "/Users/deepikaawasthi/temporal/temporal-certs/client.pem");

  public static final String KEY =
      env("TEMPORAL_KEY", "/Users/deepikaawasthi/temporal/temporal-certs/client.key");

  public static final String TASK_QUEUE = env("TASK_QUEUE", "openmetrics-task-queue");

  // default 60s; override with WORKER_SECONDS env var
  public static final int WORKER_SECONDS = envInt("WORKER_SECONDS", 60);

  public static WorkflowClient client() {
    WorkflowServiceStubs service = serviceStubs();
    return WorkflowClient.newInstance(
        service, WorkflowClientOptions.newBuilder().setNamespace(NAMESPACE).build());
  }

  // this will create servicestubs and which will be used by both workermain and starter
  private static WorkflowServiceStubs serviceStubs() {
    try (InputStream clientCert = new FileInputStream(CERT);
        InputStream clientKey = new FileInputStream(KEY)) {

      SslContext sslContext =
          GrpcSslContexts.configure(SslContextBuilder.forClient().keyManager(clientCert, clientKey))
              .build();

      WorkflowServiceStubsOptions options =
          WorkflowServiceStubsOptions.newBuilder()
              .setTarget(ADDRESS)
              .setSslContext(sslContext)
              .build();

      return WorkflowServiceStubs.newServiceStubs(options);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create Temporal TLS connection", e);
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
