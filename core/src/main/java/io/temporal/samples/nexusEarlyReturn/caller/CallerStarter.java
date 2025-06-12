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

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.earlyreturn.TransactionRequest;
import io.temporal.samples.nexusEarlyReturn.options.ClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallerStarter {
  private static final Logger logger = LoggerFactory.getLogger(CallerStarter.class);

  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient(args);

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(CallerWorker.DEFAULT_TASK_QUEUE_NAME).build();
    TransferWorkflow transferWorkflow =
        client.newWorkflowStub(TransferWorkflow.class, workflowOptions);
    WorkflowExecution execution =
        WorkflowClient.start(
            transferWorkflow::transfer, new TransactionRequest("source", "target", 100));
    logger.info(
        "Started TransferWorkflow workflowId: {} runId: {}",
        execution.getWorkflowId(),
        execution.getRunId());
    logger.info(
        "Workflow result: {}",
        transferWorkflow.transfer(new TransactionRequest("source", "target", 100)));
  }
}
