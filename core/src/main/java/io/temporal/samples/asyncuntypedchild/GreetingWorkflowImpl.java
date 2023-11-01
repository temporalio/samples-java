package io.temporal.samples.asyncuntypedchild;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

// Define the parent workflow implementation. It implements the getGreeting workflow method
public class GreetingWorkflowImpl implements GreetingWorkflow {

  @Override
  public String getGreeting(String name) {
    /*
     * Define the child workflow stub. Since workflows are stateful,
     * a new stub must be created for each child workflow.
     */
    GreetingChild child =
        Workflow.newChildWorkflowStub(
            GreetingChild.class,
            ChildWorkflowOptions.newBuilder()
                .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
                .build());

    // This is a non blocking call that returns immediately.
    // Use child.composeGreeting("Hello", name) to call synchronously.

    /*
     * Invoke the child workflows composeGreeting workflow method.
     * This call is non-blocking and returns immediately returning a {@link io.temporal.workflow.Promise}
     *
     * You can use child.composeGreeting("Hello", name) instead to call the child workflow method synchronously.
     */
    Async.function(child::composeGreeting, "Hello", name);

    // Wait for the child workflow to start before returning the result
    Promise<WorkflowExecution> childExecution = Workflow.getWorkflowExecution(child);
    // Wait for child to start
    WorkflowExecution childWorkflowExecution = childExecution.get();

    // return the child workflowId
    return childWorkflowExecution.getWorkflowId();
  }
}
