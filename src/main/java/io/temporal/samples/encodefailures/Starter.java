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

package io.temporal.samples.encodefailures;

import io.temporal.api.common.v1.Payload;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import java.util.Collections;

public class Starter {
  private static final String TASK_QUEUE = "EncodeDecodeFailuresTaskQueue";
  private static final String WORKFLOW_ID = "CustomerValidationWorkflow";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    // CodecDataConverter defines our data converter and codec
    // sets encodeFailureAttributes to true
    CodecDataConverter codecDataConverter =
        new CodecDataConverter(
            // For sample we just use default data converter
            DefaultDataConverter.newDefaultInstance(),
            // Simple prefix codec to encode/decode
            Collections.singletonList(new SimplePrefixPayloadCodec()),
            true); // Setting encodeFailureAttributes to true

    // WorkflowClient uses our CodecDataConverter
    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder().setDataConverter(codecDataConverter).build());

    // Create worker and start Worker factory
    createWorker(client);

    // Start workflow execution and catch client error (workflow execution fails)
    CustomerAgeCheck workflow =
        client.newWorkflowStub(
            CustomerAgeCheck.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    try {
      // Start workflow execution to validate under-age customer
      workflow.validateCustomer(new MyCustomer("John", 17));
      System.out.println("Workflow should have failed on customer validation");
    } catch (WorkflowFailedException e) {
      // Get failure message from last event in history (WorkflowExecutionFailed event) and check
      // that
      // its encoded
      HistoryEvent wfExecFailedEvent = client.fetchHistory(WORKFLOW_ID).getLastEvent();
      Payload payload =
          wfExecFailedEvent
              .getWorkflowExecutionFailedEventAttributes()
              .getFailure()
              .getEncodedAttributes();
      if (isEncoded(payload)) {
        System.out.println("Workflow failure was encoded");
      } else {
        System.out.println("Workflow failure was not encoded");
      }
    }

    // Stop sample
    System.exit(0);
  }

  private static boolean isEncoded(Payload payload) {
    return payload.getData().startsWith(SimplePrefixPayloadCodec.PREFIX);
  }

  private static void createWorker(WorkflowClient client) {
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(
        WorkflowImplementationOptions.newBuilder()
            // note we set InvalidCustomerException to fail execution
            .setFailWorkflowExceptionTypes(InvalidCustomerException.class)
            .build(),
        CustomerAgeCheckImpl.class);
    factory.start();
  }
}
