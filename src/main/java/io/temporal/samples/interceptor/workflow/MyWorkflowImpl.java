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

package io.temporal.samples.interceptor.workflow;

import io.temporal.workflow.Workflow;

public class MyWorkflowImpl implements MyWorkflow {

  private String name;
  private String title;
  private boolean exit;

  @Override
  public String exec() {

    // wait for a greeting
    Workflow.await(() -> name != null && title != null);

    // execute child workflow
    MyChildWorkflow child = Workflow.newChildWorkflowStub(MyChildWorkflow.class);
    String result = child.execChild(name, title);

    // wait for exit signal
    Workflow.await(() -> exit == true);

    return result;
  }

  @Override
  public void signalNameAndTitle(String name, String title) {
    this.name = name;
    this.title = title;
  }

  @Override
  public String queryName() {
    return name;
  }

  @Override
  public String queryTitle() {
    return title;
  }

  @Override
  public void exit() {
    this.exit = true;
  }
}
