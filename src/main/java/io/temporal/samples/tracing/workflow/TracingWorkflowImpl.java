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

package io.temporal.samples.tracing.workflow;

import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;

public class TracingWorkflowImpl implements TracingWorkflow {

  private String language = "English";

  @Override
  public String greet(String name) {
    ChildWorkflowOptions options =
        ChildWorkflowOptions.newBuilder().setWorkflowId("tracingChildWorkflow").build();

    // Get the child workflow stub
    TracingChildWorkflow child = Workflow.newChildWorkflowStub(TracingChildWorkflow.class, options);

    // Invoke child sync and return its result
    return child.greet(name, language);
  }

  @Override
  public void setLanguage(String language) {
    this.language = language;
  }

  @Override
  public String getLanguage() {
    return language;
  }
}
