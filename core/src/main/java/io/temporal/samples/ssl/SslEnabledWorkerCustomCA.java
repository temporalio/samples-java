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

package io.temporal.samples.ssl;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import java.io.FileInputStream;
import java.io.InputStream;

public class SslEnabledWorkerCustomCA {

  static final String TASK_QUEUE = "MyTaskQueue";

  public static void main(String[] args) throws Exception {

    // Load your client certificate
    InputStream clientCert = new FileInputStream(System.getenv("TEMPORAL_CLIENT_CERT"));

    // PKCS8 client key
    InputStream clientKey = new FileInputStream(System.getenv("TEMPORAL_CLIENT_KEY"));

    // Certification Authority certificate
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
    // communicate with
    // backend.
    // client that can be used to start and signal workflows.
    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setNamespace(namespace).build());

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);

    // Worker that listens on a task queue and hosts both workflow and activity
    // implementations.
    factory.newWorker(TASK_QUEUE);

    // TODO: now register your workflow types and activity implementations.
    // worker.registerWorkflowImplementationTypes(...);
    // worker.registerActivitiesImplementations(...);
    factory.start();
  }
}
