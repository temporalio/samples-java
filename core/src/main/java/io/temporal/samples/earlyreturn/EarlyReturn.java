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

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.*;
import io.temporal.failure.ApplicationFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EarlyReturn {
  private static final String TASK_QUEUE = "EarlyReturnTaskQueue";
  private static final String UPDATE_NAME = "early-return";

  public static void main(String[] args) {
    WorkflowClient client = setupWorkflowClient();
    startWorker(client);
    runWorkflowWithUpdateWithStart(client);
  }

  private static WorkflowClient setupWorkflowClient() {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    return WorkflowClient.newInstance(service);
  }

  private static void startWorker(WorkflowClient client) {
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);

    worker.registerWorkflowImplementationTypes(TransactionWorkflowImpl.class);
    worker.registerActivitiesImplementations(new TransactionActivitiesImpl());

    factory.start();
    System.out.println("Worker started");
  }

  private static void runWorkflowWithUpdateWithStart(WorkflowClient client) {
    Transaction tx =
        new Transaction(
            "Bob",
            "Alice",
            // Change this amount to a negative number to have initTransaction fail
            10000);
    WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setWorkflowId("early-return-workflow-" + System.currentTimeMillis())
            .build();

    WorkflowStub workflowStub = client.newUntypedWorkflowStub("TransactionWorkflow", options);

    try {
      System.out.println("Starting workflow with UpdateWithStart");

      UpdateWithStartWorkflowOperation<String> update =
          UpdateWithStartWorkflowOperation.newBuilder(UPDATE_NAME, String.class, new Object[] {})
              .setWaitForStage(WorkflowUpdateStage.COMPLETED)
              .build();

      WorkflowUpdateHandle<String> updateHandle = workflowStub.updateWithStart(update, tx);
      String transactionId = updateHandle.getResultAsync().get();
      System.out.println("Transaction initialized successfully: " + transactionId);

      // The workflow will continue running, completing the transaction.
      String result = workflowStub.getResult(String.class);
      System.out.println("Workflow completed with result: " + result);
    } catch (Exception e) {
      System.out.println("Error during workflow execution: " + e.getCause());
      // The workflow will continue running, cancelling the transaction.
    }
  }

  @WorkflowInterface
  public interface TransactionWorkflow {
    @WorkflowMethod
    String processTransaction(Transaction tx);

    @UpdateMethod(name = UPDATE_NAME)
    String returnInitResult();
  }

  public static class TransactionWorkflowImpl implements TransactionWorkflow {
    private static final Logger log = LoggerFactory.getLogger(TransactionWorkflowImpl.class);
    private final TransactionActivities activities =
        Workflow.newActivityStub(
            TransactionActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30)).build());

    private boolean initDone = false;
    private Transaction tx;
    private Exception initError = null;

    @Override
    public String processTransaction(Transaction txInput) {
      this.tx = txInput;
      // Phase 1: Initialize the transaction
      try {
        this.tx = activities.initTransaction(this.tx);
      } catch (Exception e) {
        initError = e;
      } finally {
        initDone = true;
      }

      // Phase 2: Complete or cancel the transaction
      if (initError != null) {
        activities.cancelTransaction(this.tx);
        return "Transaction cancelled";
      } else {
        activities.completeTransaction(this.tx);
        return "Transaction completed successfully: " + this.tx.id;
      }
    }

    @Override
    public String returnInitResult() {
      Workflow.await(() -> initDone);
      if (initError != null) {
        log.info("Initialization failed.");
        throw Workflow.wrap(initError);
      }
      return tx.getId();
    }
  }

  @ActivityInterface
  public interface TransactionActivities {
    @ActivityMethod
    Transaction initTransaction(Transaction tx);

    @ActivityMethod
    void cancelTransaction(Transaction tx);

    @ActivityMethod
    void completeTransaction(Transaction tx);
  }

  public static class TransactionActivitiesImpl implements TransactionActivities {
    @Override
    public Transaction initTransaction(Transaction tx) {
      System.out.println("Initializing transaction");
      sleep(500);
      if (tx.getAmount() <= 0) {
        System.out.println("Invalid amount: " + tx.getAmount());
        throw ApplicationFailure.newNonRetryableFailure(
            "Non-retryable Activity Failure: Invalid Amount", "InvalidAmount");
      }
      // mint a transaction ID
      String transactionId =
          "TXID" + String.format("%010d", (long) (Math.random() * 1_000_000_0000L));
      tx.setId(transactionId);
      sleep(500);
      return tx;
    }

    @Override
    public void cancelTransaction(Transaction tx) {
      System.out.println("Cancelling transaction");
      sleep(2000);
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

  public static class Transaction {
    private String id;
    private String sourceAccount;
    private String targetAccount;
    private int amount;

    // No-arg constructor for Jackson deserialization
    public Transaction() {}

    public Transaction(String sourceAccount, String targetAccount, int amount) {
      this.sourceAccount = sourceAccount;
      this.targetAccount = targetAccount;
      this.amount = amount;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getSourceAccount() {
      return sourceAccount;
    }

    public void setSourceAccount(String sourceAccount) {
      this.sourceAccount = sourceAccount;
    }

    public String getTargetAccount() {
      return targetAccount;
    }

    public void setTargetAccount(String targetAccount) {
      this.targetAccount = targetAccount;
    }

    public int getAmount() {
      return amount;
    }

    public void setAmount(int amount) {
      this.amount = amount;
    }
  }
}
