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

package io.temporal.samples.updatewithstart;

import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateWithStartWorkflowImpl implements UpdateWithStartWorkflow {
  private static final Logger log = LoggerFactory.getLogger(UpdateWithStartWorkflowImpl.class);
  private UpdateWithStartWorkflowState state;

  @WorkflowInit
  public UpdateWithStartWorkflowImpl(StartWorkflowRequest args) {
    this.state = new UpdateWithStartWorkflowState();
    this.state.setInitArgs(args);
    System.out.println("WorkflowInit args = " + args);
  }

  @Override
  public void execute(StartWorkflowRequest args) {
    log.info("Workflow started {}", args);

    this.state.setExecuteArgs(args);
    System.out.println("execute called");

    Workflow.await(() -> this.state.getUpdates().size() == 2);
  }

  @Override
  public UpdateWithStartWorkflowState putApplication(StartWorkflowRequest args) {

    this.state.getUpdates().add(args);
    System.out.println("put application called " + this.state.getUpdates().size());
    return this.state;
  }

  @Override
  public UpdateWithStartWorkflowState getState() {
    System.out.println("getState called " + this.state.getInitArgs().getValue());
    return this.state;
  }
}
