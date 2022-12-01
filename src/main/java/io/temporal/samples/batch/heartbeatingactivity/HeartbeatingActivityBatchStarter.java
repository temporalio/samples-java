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

package io.temporal.samples.batch.heartbeatingactivity;

import static io.temporal.samples.batch.heartbeatingactivity.HeartbeatingActivityBatchWorker.TASK_QUEUE;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

/** Starts a single execution of HeartbeatingActivityBatchWorkflow. */
public class HeartbeatingActivityBatchStarter {

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient workflowClient = WorkflowClient.newInstance(service);
    WorkflowOptions options = WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build();
    HeartbeatingActivityBatchWorkflow batchWorkflow =
        workflowClient.newWorkflowStub(HeartbeatingActivityBatchWorkflow.class, options);
    WorkflowExecution execution = WorkflowClient.start(batchWorkflow::processBatch);
    System.out.println(
        "Started batch workflow. WorkflowId="
            + execution.getWorkflowId()
            + ", RunId="
            + execution.getRunId());
    System.exit(0);
  }
}
