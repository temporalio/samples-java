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

package io.temporal.samples.moneybatch;

import io.temporal.client.BatchRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.Random;
import java.util.UUID;

public class TransferRequester {

  /** Number of withdrawals to batch */
  public static final int BATCH_SIZE = 3;

  @SuppressWarnings("CatchAndPrintStackTrace")
  public static void main(String[] args) {
    String reference = UUID.randomUUID().toString();
    int amountCents = (new Random().nextInt(5) + 1) * 25;
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient workflowClient = WorkflowClient.newInstance(service);

    String from = "account1";
    String to = "account2";
    WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setTaskQueue(AccountActivityWorker.TASK_QUEUE)
            .setWorkflowId(to)
            .build();
    AccountTransferWorkflow transferWorkflow =
        workflowClient.newWorkflowStub(AccountTransferWorkflow.class, options);
    // Signal with start sends a signal to a workflow starting it if not yet running
    BatchRequest request = workflowClient.newSignalWithStartRequest();
    request.add(transferWorkflow::deposit, to, BATCH_SIZE);
    request.add(transferWorkflow::withdraw, from, reference, amountCents);
    workflowClient.signalWithStart(request);

    System.out.printf("Transfer of %d cents from %s to %s requested", amountCents, from, to);
    System.exit(0);
  }
}
