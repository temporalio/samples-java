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

package io.temporal.samples.sleepfordays;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

public class Starter {

  public static final String TASK_QUEUE = "SleepForDaysTaskQueue";

  public static void main(String[] args) {
    // Start a workflow execution.
    SleepForDaysWorkflow workflow =
        Worker.client.newWorkflowStub(
            SleepForDaysWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

    // Start the workflow.
    WorkflowClient.start(workflow::sleepForDays);

    WorkflowStub stub = WorkflowStub.fromTyped(workflow);

    // Wait for workflow to complete. This will wait indefinitely until a 'complete' signal is sent.
    stub.getResult(String.class);
    System.exit(0);
  }
}
