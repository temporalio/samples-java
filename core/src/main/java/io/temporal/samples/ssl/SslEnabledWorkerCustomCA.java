package io.temporal.samples.ssl;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.FileInputStream;
import java.io.InputStream;

public class SslEnabledWorkerCustomCA {

  static final String TASK_QUEUE = "MyTaskQueue";

  public static void main(String[] args) throws Exception {

    // Load your client certificate, which should look like:
    // -----BEGIN CERTIFICATE-----
    // ...
    // -----END CERTIFICATE-----
    InputStream clientCert = new FileInputStream(System.getenv("TEMPORAL_CLIENT_CERT"));

    // PKCS8 client key, which should look like:
    // -----BEGIN PRIVATE KEY-----
    // ...
    // -----END PRIVATE KEY-----
    InputStream clientKey = new FileInputStream(System.getenv("TEMPORAL_CLIENT_KEY"));

    // Load your Certification Authority certificate, which should look like:
    // -----BEGIN CERTIFICATE-----
    // ...
    // -----END CERTIFICATE-----
    InputStream caCert = new FileInputStream(System.getenv("TEMPORAL_CA_CERT"));

    // For temporal cloud this would likely be ${namespace}.tmprl.cloud:7233
    String targetEndpoint = System.getenv("TEMPORAL_ENDPOINT");

    // Your registered namespace.
    String namespace = System.getenv("TEMPORAL_NAMESPACE");

    // Create an SSL Context using the client certificate and key based on the implementation
    // SimpleSslContextBuilder
    // https://github.com/temporalio/sdk-java/blob/master/temporal-serviceclient/src/main/java/io/temporal/serviceclient/SimpleSslContextBuilder.java
    SslContext sslContext =
        GrpcSslContexts.configure(
                SslContextBuilder.forClient()
                    .keyManager(clientCert, clientKey)
                    .trustManager(caCert))
            .build();

    // Create SSL enabled client by passing SslContext, created by
    // SimpleSslContextBuilder.
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setSslContext(sslContext)
                .setTarget(targetEndpoint)
                // Override the authority name used for TLS handshakes
                .setChannelInitializer(
                    c -> c.overrideAuthority(System.getenv("TEMPORAL_SERVER_HOSTNAME")))
                .build());

    // Now setup and start workflow worker, which uses SSL enabled gRPC service to
    // communicate with backend. client that can be used to start and signal workflows.
    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setNamespace(namespace).build());

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);

    /*
     * Define the workflow worker. Workflow workers listen to a defined task queue and process
     * workflows and activities.
     */
    Worker worker = factory.newWorker(TASK_QUEUE);

    /*
     * Register our workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(MyWorkflowImpl.class);

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    // Create the workflow client stub. It is used to start our workflow execution.
    MyWorkflow workflow =
        client.newWorkflowStub(
            MyWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("WORKFLOW_ID")
                .setTaskQueue(TASK_QUEUE)
                .build());

    /*
     * Execute our workflow and wait for it to complete. The call to our execute method is
     * synchronous.
     */
    String greeting = workflow.execute();

    // Display workflow execution results
    System.out.println("Workflow completed:" + greeting);
    System.exit(0);
  }
}
