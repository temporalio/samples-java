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

package io.temporal.samples.asyncuntypedchild;

import static io.temporal.samples.asyncuntypedchild.Starter.WORKFLOW_ID;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.workflow.*;

// Define the parent workflow implementation. It implements the getGreeting workflow method
public class ParentWorkflowImpl implements ParentWorkflow {

  @Override
  public String getGreeting(String name) {
    /*
     * Define the child workflow stub. Since workflows are stateful,
     * a new stub must be created for each child workflow.
     */
    ChildWorkflowStub child =
        Workflow.newUntypedChildWorkflowStub(
            ChildWorkflow.class.getSimpleName(),
            ChildWorkflowOptions.newBuilder()
                .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
                .setWorkflowId("Child_of_" + WORKFLOW_ID)
                .build());

    /*
     * Invoke the child workflows composeGreeting workflow method async.
     * This call is non-blocking and returns immediately returning a {@link io.temporal.workflow.Promise},
     * you can invoke `get()` on the returned promise to wait for the child workflow result.
     */
    child.executeAsync(String.class, "Hello", name);

    // Wait for the child workflow to start before returning the result
    Promise<WorkflowExecution> childExecution = child.getExecution();
    WorkflowExecution childWorkflowExecution = childExecution.get();

    // return the child workflowId
    return childWorkflowExecution.getWorkflowId();
  }
}
