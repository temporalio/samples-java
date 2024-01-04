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

public class Task {

  private String token;
  private TaskData data;
  private State state;

  public Task() {}

  public Task(String token) {
    this.token = token;
    this.state = State.pending;
  }

  public String getToken() {
    return token;
  }

  public void setData(TaskData data) {
    this.data = data;
  }

  public TaskData getData() {
    return data;
  }

  public void setState(State state) {
    this.state = state;
  }

  public State getState() {
    return state;
  }

  @JsonIgnore
  public boolean isCompleted() {
    return State.completed == this.state;
  }

  @Override
  public String toString() {
    return "Task{" + "token='" + token + '\'' + ", data=" + data + ", state=" + state + '}';
  }

  public enum State {
    pending,
    started,
    completed
  }

  public static class TaskData {
    private String value;

    public TaskData() {}

    public TaskData(final String value) {

      this.value = value;
    }

    public void setValue(final String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return "TaskData{" + "value='" + value + '\'' + '}';
    }
  }
}