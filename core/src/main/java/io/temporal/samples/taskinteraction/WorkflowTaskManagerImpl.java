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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

public class WorkflowTaskManagerImpl implements WorkflowTaskManager {

  private PendingTasks pendingTasks = new PendingTasks();

  @Override
  public void execute(final PendingTasks inputPendingTasks) {

    initTaskList(inputPendingTasks);

    Workflow.await(() -> Workflow.getInfo().isContinueAsNewSuggested());

    Workflow.newContinueAsNewStub(WorkflowTaskManager.class).execute(this.pendingTasks);
  }

  @Override
  public void createTask(Task task) {
    initTaskList(new PendingTasks());
    pendingTasks.addTask(task);
  }

  @Override
  public void completeTaskByToken(String taskToken) {

    Task task = this.pendingTasks.filterTaskByToken(taskToken).get();

    final String externalWorkflowId = extractWorkflowIdFromTaskToken(taskToken);

    // Signal back to the workflow that started this task to notify that the task was completed
    Workflow.newExternalWorkflowStub(TaskClient.class, externalWorkflowId)
        .completeTaskByToken(taskToken);

    this.pendingTasks.markTaskAsCompleted(task);
  }

  @Override
  public List<Task> getPendingTask() {
    return pendingTasks.getTasks();
  }

  private void initTaskList(final PendingTasks pendingTasks) {
    this.pendingTasks = this.pendingTasks == null ? new PendingTasks() : this.pendingTasks;

    // Update method addTask can be invoked before the main workflow method.
    if (pendingTasks != null) {
      this.pendingTasks.addAll(pendingTasks.getTasks());
    }
  }

  private String extractWorkflowIdFromTaskToken(final String taskToken) {
    return new StringTokenizer(taskToken, "_").nextToken();
  }

  public static class PendingTasks {
    private final List<Task> tasks;

    public PendingTasks() {
      this(new ArrayList<>());
    }

    public PendingTasks(final List<Task> tasks) {
      this.tasks = tasks;
    }

    public void addTask(final Task task) {
      this.tasks.add(task);
    }

    public void addAll(final List<Task> tasks) {
      this.tasks.addAll(tasks);
    }

    public void markTaskAsCompleted(final Task task) {
      // For the sake of simplicity, we delete the task if it is marked as completed.
      // Nothing stops us from having a field to track the tasks' state
      tasks.remove(task);
    }

    private Optional<Task> filterTaskByToken(final String taskToken) {
      return tasks.stream().filter((t) -> t.getToken().equals(taskToken)).findFirst();
    }

    private List<Task> getTasks() {
      return tasks;
    }
  }
}
