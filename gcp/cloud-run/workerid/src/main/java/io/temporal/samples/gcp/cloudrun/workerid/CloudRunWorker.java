package io.temporal.samples.gcp.cloudrun.workerid;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.gcp.cloudrun.workerid.GoogleCloudRunMetadata;
import io.temporal.gcp.cloudrun.workerid.WorkerIdPlugin;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A continuously polling Temporal Worker for a Google Cloud Run worker pool. */
public final class CloudRunWorker {
  private static final Logger logger = LoggerFactory.getLogger(CloudRunWorker.class);

  static final String ADDRESS_ENV = "TEMPORAL_ADDRESS";
  static final String NAMESPACE_ENV = "TEMPORAL_NAMESPACE";
  static final String TASK_QUEUE_ENV = "TEMPORAL_TASK_QUEUE";

  static final String DEFAULT_ADDRESS = "127.0.0.1:7233";
  static final String DEFAULT_NAMESPACE = "default";
  static final String DEFAULT_TASK_QUEUE = "cloud-run-worker-id";

  private CloudRunWorker() {}

  public static void main(String[] args) {
    // Read Cloud Run instance metadata once during startup. This performs a single HTTP request to
    // the Cloud Run metadata server and throws IllegalStateException when it is unreachable, which
    // usually means the process is not running on Google Cloud Run.
    GoogleCloudRunMetadata metadata = GoogleCloudRunMetadata.fetch();

    String address = envOrDefault(ADDRESS_ENV, DEFAULT_ADDRESS);
    String namespace = envOrDefault(NAMESPACE_ENV, DEFAULT_NAMESPACE);
    String taskQueue = envOrDefault(TASK_QUEUE_ENV, DEFAULT_TASK_QUEUE);

    // Plaintext connection to the Temporal Service. Configure TLS or an API key here for a secured
    // Service such as Temporal Cloud.
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder().setTarget(address).build());

    // Register WorkerIdPlugin on the client. It sets the derived worker identity
    // ({instanceId}@{revision}) on the client, and workers created from the client inherit it.
    // Passing the already-fetched metadata avoids a second call to the Cloud Run metadata server.
    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder()
                .setNamespace(namespace)
                .setPlugins(new WorkerIdPlugin(metadata))
                .build());

    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(taskQueue);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    Runtime.getRuntime()
        .addShutdownHook(new Thread(() -> shutdown(factory, service), "temporal-worker-shutdown"));

    factory.start();
    logger.info(
        "Temporal worker started (identity={}, taskQueue={})",
        metadata.workerIdentity(),
        taskQueue);

    // Cloud Run worker pools are continuous workloads, so keep the process alive until SIGTERM.
    factory.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  private static void shutdown(WorkerFactory factory, WorkflowServiceStubs service) {
    // Cloud Run sends SIGTERM and allows a short grace period before SIGKILL. Stop polling, drain
    // in-flight tasks, then close the service connection.
    factory.shutdown();
    factory.awaitTermination(6, TimeUnit.SECONDS);
    if (!factory.isTerminated()) {
      factory.shutdownNow();
      factory.awaitTermination(1, TimeUnit.SECONDS);
    }
    service.shutdown();
  }

  private static String envOrDefault(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value;
  }
}
