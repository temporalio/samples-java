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

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class AccountTransferWorkflowImpl implements AccountTransferWorkflow {

  private final ActivityOptions options =
      ActivityOptions.newBuilder()
          .setStartToCloseTimeout(Duration.ofSeconds(5))
          .setRetryOptions(
              RetryOptions.newBuilder()
                  .setInitialInterval(Duration.ofSeconds(1))
                  .setMaximumInterval(Duration.ofSeconds(10))
                  .build())
          .build();

  private final Account account = Workflow.newActivityStub(Account.class, options);

  private Set<String> references = new HashSet<>();
  private int balance;
  private int count;

  @Override
  public void deposit(String toAccount, int batchSize) {
    Workflow.await(() -> count == batchSize);
    String referenceId = Workflow.randomUUID().toString();
    account.deposit(toAccount, referenceId, balance);
  }

  @Override
  public void withdraw(String fromAccountId, String referenceId, int amountCents) {
    if (!references.add(referenceId)) {
      return; // duplicate
    }
    account.withdraw(fromAccountId, referenceId, amountCents);
    balance += amountCents;
    count++;
  }

  @Override
  public int getBalance() {
    return balance;
  }

  @Override
  public int getCount() {
    return count;
  }
}
