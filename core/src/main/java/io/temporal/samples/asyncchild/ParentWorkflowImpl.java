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
