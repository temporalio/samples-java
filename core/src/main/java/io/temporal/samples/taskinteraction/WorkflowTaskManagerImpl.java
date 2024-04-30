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

import io.temporal.workflow.Workflow;
import java.util.*;

public class WorkflowTaskManagerImpl implements WorkflowTaskManager {

  private List<Task> pendingTask;

  private List<String> tasksToComplete;

  @Override
  public void execute(List<Task> inputPendingTask, List<String> inputTaskToComplete) {
    initPendingTasks(inputPendingTask);
    initTaskToComplete(inputTaskToComplete);

    while (true) {

      Workflow.await(
          () ->
              // Wait until there are pending task to complete
              !tasksToComplete.isEmpty());

      final String taskToken = tasksToComplete.remove(0);

      // Find the workflow id of the workflow we have to signal back
      final String externalWorkflowId = new StringTokenizer(taskToken, "_").nextToken();

      Workflow.newExternalWorkflowStub(TaskClient.class, externalWorkflowId)
          .completeTaskByToken(taskToken);

      final Task task = getPendingTaskWithToken(taskToken).get();
      pendingTask.remove(task);

      if (Workflow.getInfo().isContinueAsNewSuggested()) {
        Workflow.newContinueAsNewStub(WorkflowTaskManager.class)
            .execute(pendingTask, tasksToComplete);
      }
    }
  }

  @Override
  public void createTask(Task task) {
    initPendingTasks(new ArrayList<>());
    pendingTask.add(task);
  }

  @Override
  public void completeTaskByToken(String taskToken) {

    tasksToComplete.add(taskToken);

    Workflow.await(
        () -> {
          final boolean taskCompleted =
              getPendingTask().stream().noneMatch((t) -> Objects.equals(t.getToken(), taskToken));

          return taskCompleted;
        });
  }

  @Override
  public List<Task> getPendingTask() {
    return pendingTask;
  }

  private Optional<Task> getPendingTaskWithToken(final String taskToken) {
    return pendingTask.stream().filter((t) -> t.getToken().equals(taskToken)).findFirst();
  }

  private void initTaskToComplete(final List<String> tasks) {
    if (tasksToComplete == null) {
      tasksToComplete = new ArrayList<>();
    }
    tasksToComplete.addAll(tasks);
  }

  private void initPendingTasks(final List<Task> tasks) {

    if (pendingTask == null) {
      pendingTask = new ArrayList<>();
    }
    pendingTask.addAll(tasks);
  }
}
