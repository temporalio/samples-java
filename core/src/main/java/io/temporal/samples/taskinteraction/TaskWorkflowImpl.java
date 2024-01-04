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

package io.temporal.samples.taskinteraction;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Arrays;
import org.slf4j.Logger;

public class TaskWorkflowImpl implements TaskWorkflow {

  private final Logger logger = Workflow.getLogger(TaskWorkflowImpl.class);

  private final TaskService<String> taskService = new TaskService<>();

  private final TaskActivity activity =
      Workflow.newActivityStub(
          TaskActivity.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

  @Override
  public void execute() {
    final TaskToken taskToken = new TaskToken();

    // Schedule two "tasks" in parallel. The last parameter is the token the client needs
    // to change the task state, and ultimately to complete the task
    logger.info("About to create async tasks");
    final Promise<String> task1 =
        taskService.executeTaskAsync(() -> activity.createTask("TODO 1"), taskToken.getNext());
    final Promise<String> task2 =
        taskService.executeTaskAsync(() -> activity.createTask("TODO 2"), taskToken.getNext());

    logger.info("Awaiting for two tasks to get completed");
    // Block execution until both tasks complete
    Promise.allOf(Arrays.asList(task1, task2)).get();
    logger.info("Two tasks completed");

    logger.info("About to create one blocking task");
    // Blocking invocation
    taskService.executeTask(() -> activity.createTask("TODO 3"), taskToken.getNext());
    logger.info("Task completed");
    logger.info("Completing workflow");
  }

  private static class TaskToken {

    private int taskToken = 1;

    public String getNext() {

      return Workflow.getInfo().getWorkflowId()
          + "-"
          + Workflow.currentTimeMillis()
          + "-"
          + taskToken++;
    }
  }
}
