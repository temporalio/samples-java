

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
