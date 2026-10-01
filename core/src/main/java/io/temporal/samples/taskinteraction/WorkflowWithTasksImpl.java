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

import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.util.Arrays;
import org.slf4j.Logger;

/** Workflow that creates three task and waits for them to complete */
public class WorkflowWithTasksImpl implements WorkflowWithTasks {

  private final Logger logger = Workflow.getLogger(WorkflowWithTasksImpl.class);

  private final TaskService<Void> taskService = new TaskService<>();

  @Override
  public void execute() {

    // Schedule two "tasks" in parallel. The last parameter is the token the client needs
    // to unblock/complete the task. This token contains the workflowId that the
    // client can use to create the workflow stub.
    final TaskToken taskToken = new TaskToken();

    logger.info("About to create async tasks");
    final Promise<Void> task1 =
        Async.procedure(
            () -> {
              final Task task = new Task(taskToken.getNext(), new Task.TaskTitle("TODO 1"));
              taskService.executeTask(task);
            });

    final Promise<Void> task2 =
        Async.procedure(
            () -> {
              final Task task = new Task(taskToken.getNext(), new Task.TaskTitle("TODO 2"));
              taskService.executeTask(task);
            });

    logger.info("Awaiting for the two tasks to complete");
    // Block execution until both tasks complete
    Promise.allOf(Arrays.asList(task1, task2)).get();
    logger.info("Tasks completed");

    // Blocking invocation
    taskService.executeTask(new Task(taskToken.getNext(), new Task.TaskTitle("TODO 3")));
    logger.info("Completing workflow");
  }

  private static class TaskToken {
    private int taskToken = 1;

    public String getNext() {

      return Workflow.getInfo().getWorkflowId() + "_" + taskToken++;
    }
  }
}
