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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.temporal.workflow.CompletablePromise;
import io.temporal.workflow.Workflow;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;

public class TaskService<R> {

  private final Map<String, Task> tasks = new HashMap<>();
  private final Map<String, CompletablePromise<R>> pendingPromises =
      Collections.synchronizedMap(new HashMap<>());

  // Exposes signal and query methods that
  // allow us to interact with the workflow execution
  private final TaskClient listener =
      new TaskClient() {

        @Override
        public void updateTask(TaskRequest taskRequest) {

          final String token = taskRequest.getToken();
          final String data = taskRequest.getData();
          tasks.get(token).setData(data);

          final Task t = tasks.get(token);

          t.setState(taskRequest.state);
          tasks.put(t.getToken(), t);

          logger.info("Task updated: " + t);

          if (taskRequest.state == Task.State.completed) {
            final CompletablePromise<R> completablePromise = pendingPromises.get(token);
            completablePromise.complete((R) data);
          }
        }

        @Override
        public List<Task> getOpenTasks() {
          return tasks.values().stream().filter(t -> !t.isCompleted()).collect(Collectors.toList());
        }
      };

  public TaskService() {
    Workflow.registerListener(listener);
  }

  private final Logger logger = Workflow.getLogger(TaskService.class);

  public R executeTask(Callback<R> callback, String token) {

    final Task task = new Task(token);
    logger.info("Before creating task : " + task);
    tasks.put(token, task);
    callback.execute();
    logger.info("Task created: " + task);

    final CompletablePromise<R> promise = Workflow.newPromise();
    pendingPromises.put(token, promise);

    return promise.get();
  }

  public List<Task> getOpenTasks() {
    return listener.getOpenTasks();
  }

  public interface Callback<T> {
    T execute();
  }

  public static class TaskRequest {

    private Task.State state;
    private String data;
    private String token;

    public TaskRequest() {}

    public TaskRequest(Task.State state, String data, String token) {
      this.state = state;
      this.data = data;
      this.token = token;
    }

    @JsonIgnore
    public boolean isCompleted() {
      return this.state == Task.State.completed;
    }

    public String getToken() {
      return token;
    }

    public String getData() {
      return data;
    }
  }
}
