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

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.SimpleSslContextBuilder;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.FileInputStream;
import java.io.InputStream;

public class SslEnabledWorker {

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
    // For temporal cloud this would likely be ${namespace}.tmprl.cloud:7233
    String targetEndpoint = System.getenv("TEMPORAL_ENDPOINT");
    // Your registered namespace.
    String namespace = System.getenv("TEMPORAL_NAMESPACE");
    // Create SSL enabled client by passing SslContext, created by SimpleSslContextBuilder.
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newInstance(
            WorkflowServiceStubsOptions.newBuilder()
                .setSslContext(SimpleSslContextBuilder.newBuilder(clientCert, clientKey).build())
                .setTarget(targetEndpoint)
                .build());

    // Now setup and start workflow worker, which uses SSL enabled gRPC service to communicate with
    // backend.
    // client that can be used to start and signal workflows.
    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // Worker that listens on a task queue and hosts both workflow and activity implementations.
    Worker worker = factory.newWorker(TASK_QUEUE);
    // TODO now register your workflow types and activity implementations.
    // worker.registerWorkflowImplementationTypes(...);
    // worker.registerActivitiesImplementations(...);
    factory.start();
  }
}
