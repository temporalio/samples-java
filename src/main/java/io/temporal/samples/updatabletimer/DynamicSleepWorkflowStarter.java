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

package io.temporal.samples.updatabletimer;

import static io.temporal.samples.updatabletimer.DynamicSleepWorkflowWorker.DYNAMIC_SLEEP_WORKFLOW_ID;
import static io.temporal.samples.updatabletimer.DynamicSleepWorkflowWorker.TASK_QUEUE;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicSleepWorkflowStarter {

  private static final Logger logger = LoggerFactory.getLogger(DynamicSleepWorkflowStarter.class);

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    DynamicSleepWorkflow workflow =
        client.newWorkflowStub(
            DynamicSleepWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId(DYNAMIC_SLEEP_WORKFLOW_ID)
                .setWorkflowIdReusePolicy(
                    WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
                .build());

    try {
      // Start asynchronously
      WorkflowExecution execution =
          WorkflowClient.start(workflow::execute, System.currentTimeMillis() + 60000);
      logger.info("Workflow started: " + execution);
    } catch (WorkflowExecutionAlreadyStarted e) {
      logger.info("Workflow already running: " + e.getExecution());
    }
  }
}
