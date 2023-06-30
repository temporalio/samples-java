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

package io.temporal.samples.springboot.kafka;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import java.time.Duration;

@WorkflowImpl(taskQueues = "KafkaSampleTaskQueue")
public class MessageWorkflowImpl implements MessageWorkflow {

  private KafkaActivity activity =
      Workflow.newActivityStub(
          KafkaActivity.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());
  private String message = null;

  @Override
  public void start() {
    Workflow.await(() -> message != null);
    // simulate some steps / milestones
    activity.sendMessage(
        "Starting execution: " + Workflow.getInfo().getWorkflowId() + " with message: " + message);

    activity.sendMessage("Step 1 done...");
    activity.sendMessage("Step 2 done...");
    activity.sendMessage("Step 3 done...");

    activity.sendMessage("Completing execution: " + Workflow.getInfo().getWorkflowId());
  }

  @Override
  public void update(String message) {
    this.message = message;
  }
}
