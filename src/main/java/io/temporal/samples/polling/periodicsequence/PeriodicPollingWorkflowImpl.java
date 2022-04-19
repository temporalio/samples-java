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

package io.temporal.samples.polling.periodicsequence;

import io.temporal.samples.polling.PollingWorkflow;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;

public class PeriodicPollingWorkflowImpl implements PollingWorkflow {

  // Set some periodic poll interval, for sample we set 5 seconds
  private int pollingIntervalInSeconds = 5;

  @Override
  public String exec() {
    PollingChildWorkflow childWorkflow =
        Workflow.newChildWorkflowStub(
            PollingChildWorkflow.class,
            ChildWorkflowOptions.newBuilder().setWorkflowId("ChildWorkflowPoll").build());

    return childWorkflow.exec(pollingIntervalInSeconds);
  }
}
