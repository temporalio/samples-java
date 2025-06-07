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

package io.temporal.samples.nexusEarlyReturn.caller;

import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.NexusOperationFailure;
import io.temporal.samples.earlyreturn.TransactionRequest;
import io.temporal.samples.earlyreturn.TxResult;
import io.temporal.samples.nexusEarlyReturn.service.TransactionService;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * TransferWorkflowImpl starts a transfer and waits for it to complete (if the transaction is
 * successful or not).
 */
public class TransferWorkflowImpl implements TransferWorkflow {
  TransactionService transactionService =
      Workflow.newNexusServiceStub(
          TransactionService.class,
          NexusServiceOptions.newBuilder()
              .setOperationOptions(
                  NexusOperationOptions.newBuilder()
                      .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                      .build())
              .build());

  @Override
  public String transfer(TransactionRequest request) {
    try {
      // Start a transaction using the TransactionService Nexus service. Once the transaction is
      // started, it will run asynchronously, and we can check the result later.
      TransactionService.StartTransactionResponse r =
          transactionService.startTransaction(
              new TransactionService.StartTransactionRequest(request));
      // Note: this random sleep is to simulate some processing time before checking the result.
      // Depending on how long the sleep is, the transaction may complete before we check the
      // result.
      Workflow.sleep(Duration.ofMillis(Workflow.newRandom().nextInt(100)));
      TxResult result =
          transactionService.getTransactionResult(
              new TransactionService.GetTransactionResultRequest(r.getTransactionToken()));
      return result.getStatus();
    } catch (NexusOperationFailure of) {
      // If the operation failed, we check if it was due to a transaction failure.
      if (of.getCause() instanceof ApplicationFailure) {
        ApplicationFailure af = (ApplicationFailure) of.getCause();
        if (af.getType().equals("TransactionFailed")) {
          // If the transaction failed, we can retrieve the transaction token from the details
          // and use it to wait for the transaction to cancel.
          String transactionToken = af.getDetails().get(String.class);
          TxResult result =
              transactionService.getTransactionResult(
                  new TransactionService.GetTransactionResultRequest(transactionToken));
          return result.getStatus();
        }
      }
      throw of;
    }
  }
}
