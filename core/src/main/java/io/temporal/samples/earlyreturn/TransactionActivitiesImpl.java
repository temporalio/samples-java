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

package io.temporal.samples.earlyreturn;

import io.temporal.failure.ApplicationFailure;

public class TransactionActivitiesImpl implements TransactionActivities {

  @Override
  public Transaction mintTransactionId(TransactionRequest request) {
    System.out.println("Minting transaction ID");
    // Simulate transaction ID generation
    String txId = "TXID" + String.format("%010d", (long) (Math.random() * 1_000_000_0000L));
    sleep(100);
    System.out.println("Transaction ID minted: " + txId);
    return new Transaction(
        txId, request.getSourceAccount(), request.getTargetAccount(), request.getAmount());
  }

  @Override
  public Transaction initTransaction(Transaction tx) {
    System.out.println("Initializing transaction");
    sleep(300);
    if (tx.getAmount() <= 0) {
      System.out.println("Invalid amount: " + tx.getAmount());
      throw ApplicationFailure.newNonRetryableFailure(
          "Non-retryable Activity Failure: Invalid Amount", "InvalidAmount");
    }

    sleep(500);
    return tx;
  }

  @Override
  public void cancelTransaction(Transaction tx) {
    System.out.println("Cancelling transaction");
    sleep(300);
    System.out.println("Transaction cancelled");
  }

  @Override
  public void completeTransaction(Transaction tx) {
    System.out.println(
        "Sending $"
            + tx.getAmount()
            + " from "
            + tx.getSourceAccount()
            + " to "
            + tx.getTargetAccount());
    sleep(2000);
    System.out.println("Transaction completed successfully");
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
