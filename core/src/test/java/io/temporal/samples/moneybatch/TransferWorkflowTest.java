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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import java.util.Random;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;

public class TransferWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(AccountTransferWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testTransfer() {
    Account activities = mock(Account.class);
    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);
    testWorkflowRule.getTestEnvironment().start();

    String from = "account1";
    String to = "account2";
    int batchSize = 5;
    WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowId(to)
            .build();
    AccountTransferWorkflow transferWorkflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(AccountTransferWorkflow.class, options);
    WorkflowClient.start(transferWorkflow::deposit, to, batchSize);
    Random random = new Random();
    int total = 0;
    for (int i = 0; i < batchSize; i++) {
      int amountCents = random.nextInt(1000);
      transferWorkflow.withdraw(from, UUID.randomUUID().toString(), amountCents);
      total += amountCents;
    }
    // Wait for workflow to finish
    WorkflowStub.fromTyped(transferWorkflow).getResult(Void.class);
    verify(activities).deposit(eq("account2"), any(), eq(total));

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
