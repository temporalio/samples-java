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

package io.temporal.samples.moneytransfer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
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

    WorkflowOptions options =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();
    AccountTransferWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(AccountTransferWorkflow.class, options);

    long start = testWorkflowRule.getTestEnvironment().currentTimeMillis();
    workflow.transfer("account1", "account2", "reference1", 123);
    long duration = testWorkflowRule.getTestEnvironment().currentTimeMillis() - start;
    System.out.println("Duration hours: " + duration / 3600000);

    verify(activities).withdraw(eq("account1"), eq("reference1"), eq(123));
    verify(activities).deposit(eq("account2"), eq("reference1"), eq(123));

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
