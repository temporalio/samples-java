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

package io.temporal.samples.dsl.model;

public class FlowAction {
  private String action;
  private String compensateBy;
  private int retries;
  private int startToCloseSec;
  private int next;

  public FlowAction() {}

  public FlowAction(
      String action, String compensateBy, int retries, int startToCloseSec, int next) {
    this.action = action;
    this.compensateBy = compensateBy;
    this.retries = retries;
    this.startToCloseSec = startToCloseSec;
    this.next = next;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getCompensateBy() {
    return compensateBy;
  }

  public void setCompensateBy(String compensateBy) {
    this.compensateBy = compensateBy;
  }

  public int getRetries() {
    return retries;
  }

  public void setRetries(int retries) {
    this.retries = retries;
  }

  public int getStartToCloseSec() {
    return startToCloseSec;
  }

  public void setStartToCloseSec(int startToCloseSec) {
    this.startToCloseSec = startToCloseSec;
  }

  public int getNext() {
    return next;
  }

  public void setNext(int next) {
    this.next = next;
  }
}
