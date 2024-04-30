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

package io.temporal.samples.taskinteraction.activity;

import io.temporal.activity.Activity;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.taskinteraction.Task;
import io.temporal.samples.taskinteraction.WorkflowTaskManager;
import java.util.ArrayList;

public class ActivityTaskImpl implements ActivityTask {

  private final WorkflowClient workflowClient;

  public ActivityTaskImpl(WorkflowClient workflowClient) {
    this.workflowClient = workflowClient;
  }

  @Override
  public void createTask(Task task) {

    final String taskQueue = Activity.getExecutionContext().getInfo().getActivityTaskQueue();

    final WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setWorkflowId(WorkflowTaskManager.WORKFLOW_ID)
            .setTaskQueue(taskQueue)
            .build();

    final WorkflowTaskManager taskManager =
        workflowClient.newWorkflowStub(WorkflowTaskManager.class, workflowOptions);
    try {
      WorkflowClient.start(taskManager::execute, new ArrayList<>(), new ArrayList<>());
    } catch (WorkflowExecutionAlreadyStarted e) {
      // expected exception if workflow was started by a previous activity execution.
      // This will be handled differently once updateWithStart is implemented
    }

    // register the "task" to the external workflow that manages task lifecycle
    taskManager.createTask(task);
  }
}
