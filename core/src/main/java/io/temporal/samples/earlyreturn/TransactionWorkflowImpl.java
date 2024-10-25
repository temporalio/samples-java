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

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionWorkflowImpl implements TransactionWorkflow {
  private static final Logger log = LoggerFactory.getLogger(TransactionWorkflowImpl.class);
  private final TransactionActivities activities =
      Workflow.newActivityStub(
          TransactionActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30)).build());

  private boolean initDone = false;
  private Transaction tx;
  private Exception initError = null;

  @Override
  public TxResult processTransaction(TransactionRequest txRequest) {
    this.tx = activities.mintTransactionId(txRequest);

    try {
      this.tx = activities.initTransaction(this.tx);
    } catch (Exception e) {
      initError = e;
    } finally {
      initDone = true;
    }

    if (initError != null) {
      // If initialization failed, cancel the transaction
      activities.cancelTransaction(this.tx);
      return new TxResult("", "Transaction cancelled.");
    } else {
      activities.completeTransaction(this.tx);
      return new TxResult(this.tx.getId(), "Transaction completed successfully.");
    }
  }

  @Override
  public TxResult returnInitResult() {
    Workflow.await(() -> initDone);

    if (initError != null) {
      log.info("Initialization failed.");
      throw Workflow.wrap(initError);
    }

    return new TxResult(tx.getId(), "Initialization successful");
  }
}
