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

package io.temporal.samples.asyncchild;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

public class ParentWorkflowImpl implements ParentWorkflow {
  @Override
  public WorkflowExecution executeParent() {

    // We set the parentClosePolicy to "Abandon"
    // This will allow child workflow to continue execution after parent completes
    ChildWorkflowOptions childWorkflowOptions =
        ChildWorkflowOptions.newBuilder()
            .setWorkflowId("childWorkflow")
            .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
            .build();

    // Get the child workflow stub
    ChildWorkflow child = Workflow.newChildWorkflowStub(ChildWorkflow.class, childWorkflowOptions);
    // Start the child workflow async
    Async.function(child::executeChild);
    // Get the child workflow execution promise
    Promise<WorkflowExecution> childExecution = Workflow.getWorkflowExecution(child);
    // Call .get on the promise. This will block until the child workflow starts execution (or start
    // fails)
    return childExecution.get();
  }
}
