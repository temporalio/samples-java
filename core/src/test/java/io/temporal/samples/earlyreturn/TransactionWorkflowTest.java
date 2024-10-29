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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.temporal.client.*;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowRule;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;

public class TransactionWorkflowTest {

  private static final String SOURCE_ACCOUNT = "Bob";
  private static final String TARGET_ACCOUNT = "Alice";
  private static final String TEST_TRANSACTION_ID = "test-id-123";
  private static final int VALID_AMOUNT = 1000;
  private static final int INVALID_AMOUNT = -1000;

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(TransactionWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testUpdateWithStartValidAmount() throws Exception {
    // Mock activities
    TransactionActivities activities =
        mock(TransactionActivities.class, withSettings().withoutAnnotations());
    when(activities.initTransaction(any()))
        .thenReturn(
            new Transaction(TEST_TRANSACTION_ID, SOURCE_ACCOUNT, TARGET_ACCOUNT, VALID_AMOUNT));

    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);
    testWorkflowRule.getTestEnvironment().start();

    // Create workflow stub
    WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();
    TransactionWorkflow workflow =
        workflowClient.newWorkflowStub(
            TransactionWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    // Create update operation
    UpdateWithStartWorkflowOperation<TxResult> updateOp =
        UpdateWithStartWorkflowOperation.newBuilder(workflow::returnInitResult)
            .setWaitForStage(WorkflowUpdateStage.COMPLETED)
            .build();

    // Execute UpdateWithStart
    WorkflowUpdateHandle<TxResult> handle =
        WorkflowClient.updateWithStart(
            workflow::processTransaction,
            new TransactionRequest(SOURCE_ACCOUNT, TARGET_ACCOUNT, VALID_AMOUNT),
            updateOp);

    // Verify both update and final results
    TxResult updateResult = handle.getResultAsync().get();
    assertEquals(TEST_TRANSACTION_ID, updateResult.getTransactionId());

    TxResult finalResult = WorkflowStub.fromTyped(workflow).getResult(TxResult.class);
    assertEquals("Transaction completed successfully.", finalResult.getStatus());

    // Verify activities were calledgit
    verify(activities).mintTransactionId(any());
    verify(activities).initTransaction(any());
    verify(activities).completeTransaction(any());
    verifyNoMoreInteractions(activities);
  }

  @Test
  public void testUpdateWithStartInvalidAmount() throws Exception {
    // Mock activities
    TransactionActivities activities =
        mock(TransactionActivities.class, withSettings().withoutAnnotations());
    when(activities.initTransaction(any()))
        .thenThrow(
            ApplicationFailure.newNonRetryableFailure(
                "Non-retryable Activity Failure: Invalid Amount", "InvalidAmount"));
    doNothing().when(activities).cancelTransaction(any());

    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);
    testWorkflowRule.getTestEnvironment().start();

    // Create workflow stub with explicit ID
    WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();
    String workflowId = "test-workflow-" + UUID.randomUUID();
    WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowId(workflowId)
            .build();

    TransactionWorkflow workflow =
        workflowClient.newWorkflowStub(TransactionWorkflow.class, options);

    // Create update operation
    UpdateWithStartWorkflowOperation<TxResult> updateOp =
        UpdateWithStartWorkflowOperation.newBuilder(workflow::returnInitResult)
            .setWaitForStage(WorkflowUpdateStage.COMPLETED)
            .build();

    // Execute UpdateWithStart and expect the exception
    WorkflowServiceException exception =
        assertThrows(
            WorkflowServiceException.class,
            () ->
                WorkflowClient.updateWithStart(
                    workflow::processTransaction,
                    new TransactionRequest(SOURCE_ACCOUNT, TARGET_ACCOUNT, INVALID_AMOUNT),
                    updateOp));

    // Verify the exception chain
    assertTrue(exception.getCause() instanceof WorkflowUpdateException);
    assertTrue(exception.getCause().getCause() instanceof ActivityFailure);
    ApplicationFailure appFailure = (ApplicationFailure) exception.getCause().getCause().getCause();
    assertEquals("InvalidAmount", appFailure.getType());
    assertTrue(appFailure.getMessage().contains("Invalid Amount"));

    // Create a new stub to get the result
    TransactionWorkflow workflowById =
        workflowClient.newWorkflowStub(TransactionWorkflow.class, workflowId);
    TxResult finalResult = WorkflowStub.fromTyped(workflowById).getResult(TxResult.class);
    assertEquals("", finalResult.getTransactionId());
    assertEquals("Transaction cancelled.", finalResult.getStatus());

    // Verify activities were called in correct order
    verify(activities).mintTransactionId(any());
    verify(activities).initTransaction(any());
    verify(activities).cancelTransaction(any());
    verifyNoMoreInteractions(activities);
  }
}
