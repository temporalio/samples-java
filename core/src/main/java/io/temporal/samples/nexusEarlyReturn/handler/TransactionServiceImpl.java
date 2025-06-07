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

package io.temporal.samples.nexusEarlyReturn.handler;

import io.nexusrpc.OperationException;
import io.nexusrpc.OperationInfo;
import io.nexusrpc.handler.*;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.*;
import io.temporal.failure.ApplicationFailure;
import io.temporal.nexus.Nexus;
import io.temporal.nexus.WorkflowRunOperation;
import io.temporal.samples.earlyreturn.TransactionRequest;
import io.temporal.samples.earlyreturn.TransactionWorkflow;
import io.temporal.samples.earlyreturn.TxResult;
import io.temporal.samples.nexusEarlyReturn.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

@ServiceImpl(service = TransactionService.class)
public class TransactionServiceImpl {
  private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);
  private final Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
  private final Base64.Decoder decoder = Base64.getDecoder();

  @OperationImpl
  public OperationHandler<
          TransactionService.StartTransactionRequest, TransactionService.StartTransactionResponse>
      startTransaction() {
    return OperationHandler.sync(
        (ctx, details, request) -> {
          // Note: It is important to use a unique workflow ID for each transaction since
          // the workflow ID is used to identify the transaction in the system, and we can only get
          // the result by the workflow ID since Temporal only supports attaching to workflow by
          // workflowID.
          String workflowId = "transaction-" + details.getRequestId();
          WorkflowOptions options =
              WorkflowOptions.newBuilder()
                  .setTaskQueue(HandlerWorker.DEFAULT_TASK_QUEUE_NAME)
                  .setWorkflowIdReusePolicy(
                      WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                  .setWorkflowIdConflictPolicy(
                      WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_FAIL)
                  .setWorkflowId(workflowId)
                  .build();
          TransactionWorkflow workflow =
              Nexus.getOperationContext()
                  .getWorkflowClient()
                  .newWorkflowStub(TransactionWorkflow.class, options);
          String transactionToken = encoder.encodeToString(workflowId.getBytes(UTF_8));
          try {
            TxResult result =
                WorkflowClient.executeUpdateWithStart(
                    workflow::returnInitResult,
                    UpdateOptions.<TxResult>newBuilder().build(),
                    new WithStartWorkflowOperation<>(
                        workflow::processTransaction, request.getTransactionRequest()));
            return new TransactionService.StartTransactionResponse(transactionToken, result);
          } catch (WorkflowUpdateException e) {
            throw OperationException.failure(
                ApplicationFailure.newNonRetryableFailureWithCause(
                    "Transaction invalid, cancellation.",
                    "TransactionFailed",
                    e.getCause(),
                    transactionToken));
          }
        });
  }

  @OperationImpl
  public OperationHandler<TransactionService.GetTransactionResultRequest, TxResult>
      getTransactionResult() {
    class AlreadyStartedWorkflowOperationHandler
        implements OperationHandler<TransactionService.GetTransactionResultRequest, TxResult> {
      @Override
      public OperationStartResult<TxResult> start(
          OperationContext context,
          OperationStartDetails osd,
          TransactionService.GetTransactionResultRequest request)
          throws HandlerException, OperationException {
        String workflowId =
            new String(decoder.decode(request.getTransactionToken().getBytes(UTF_8)), UTF_8);
        WorkflowClient client = Nexus.getOperationContext().getWorkflowClient();
        // Describe the workflow to ensure it exists before attempting to attach so we
        // don't accidentally start a new workflow when trying to attach.
        WorkflowStub stub = client.newUntypedWorkflowStub(workflowId);
        try {
          WorkflowExecutionDescription desc = stub.describe();
          // If the workflow is not running, we can fetch the result directly.
          if (!desc.getStatus().equals(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING)) {
            return OperationStartResult.sync(stub.getResult(TxResult.class));
          }
        } catch (WorkflowNotFoundException e) {
          throw OperationException.failure(
              new IllegalStateException(
                  "Workflow with ID " + workflowId + " not found. Ensure it was started first.",
                  e));
        }
        // Create a workflow stub with the same ID and task queue as the original workflow.
        // With this, when we attempt to start the workflow attach to the existing workflow if it is
        // still running or fail if it has already completed.
        TransactionWorkflow workflowStub =
            client.newWorkflowStub(
                TransactionWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowIdConflictPolicy(
                        WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_USE_EXISTING)
                    .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                    .setTaskQueue(HandlerWorker.DEFAULT_TASK_QUEUE_NAME)
                    .setWorkflowId(workflowId)
                    .build());
        OperationHandler<TransactionRequest, TxResult> workflowOperation =
            WorkflowRunOperation.fromWorkflowMethod(
                (ctx, details, input) -> workflowStub::processTransaction);
        try {
          // This should always attach to an existing workflow since we know the workflow was
          // already started.
          // So the input does not matter here.
          return workflowOperation.start(context, osd, new TransactionRequest("", "", 0));
        } catch (WorkflowExecutionAlreadyStarted e) {
          // If we get this exception, it means the workflow completed between when we called
          // describe and start, now we can
          // fetch the result directly.
          logger.info("Workflow already started for ID: {}", workflowId);
          return OperationStartResult.sync(stub.getResult(TxResult.class));
        }
      }

      @Override
      public TxResult fetchResult(OperationContext context, OperationFetchResultDetails details)
          throws HandlerException {
        throw new UnsupportedOperationException("Not implemented yet");
      }

      @Override
      public OperationInfo fetchInfo(OperationContext context, OperationFetchInfoDetails details)
          throws HandlerException {
        throw new UnsupportedOperationException("Not implemented yet");
      }

      @Override
      public void cancel(OperationContext context, OperationCancelDetails details)
          throws HandlerException {}
    }
    return new AlreadyStartedWorkflowOperationHandler();
  }
}
