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

package io.temporal.samples.keymanagementencryption.awsencryptionsdk;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.samples.hello.HelloActivity;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.util.Collections;
import software.amazon.cryptography.materialproviders.IKeyring;
import software.amazon.cryptography.materialproviders.MaterialProviders;
import software.amazon.cryptography.materialproviders.model.CreateAwsKmsMultiKeyringInput;
import software.amazon.cryptography.materialproviders.model.MaterialProvidersConfig;

public class EncryptedPayloads {

  static final String TASK_QUEUE = "EncryptedPayloads";

  public static void main(String[] args) {
    // Configure your keyring. In this sample we are configuring a basic AWS KMS keyring, but the
    // AWS encryption SDK has multiple options depending on your use case.
    //
    // See more here:
    // https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/which-keyring.html
    String generatorKey = System.getenv("AWS_KEY_ARN");

    final MaterialProviders materialProviders =
        MaterialProviders.builder()
            .MaterialProvidersConfig(MaterialProvidersConfig.builder().build())
            .build();
    // Create the AWS KMS keyring
    final CreateAwsKmsMultiKeyringInput keyringInput =
        CreateAwsKmsMultiKeyringInput.builder().generator(generatorKey).build();
    final IKeyring kmsKeyring = materialProviders.CreateAwsKmsMultiKeyring(keyringInput);
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    // client that can be used to start and signal workflows
    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder()
                .setDataConverter(
                    new CodecDataConverter(
                        DefaultDataConverter.newDefaultInstance(),
                        // Create our encryption codec
                        Collections.singletonList(new KeyringCodec(kmsKeyring))))
                .build());

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // Worker that listens on a task queue and hosts both workflow and activity implementations.
    Worker worker = factory.newWorker(TASK_QUEUE);
    // Register the workflows and activities
    worker.registerWorkflowImplementationTypes(HelloActivity.GreetingWorkflowImpl.class);
    worker.registerActivitiesImplementations(new HelloActivity.GreetingActivitiesImpl());
    // Start listening to the workflow and activity task queues.
    factory.start();

    // Start a workflow execution.
    HelloActivity.GreetingWorkflow workflow =
        client.newWorkflowStub(
            HelloActivity.GreetingWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("My Secret Friend");
    System.out.println(greeting);
    System.exit(0);
  }
}
