

package io.temporal.samples.asyncuntypedchild;

import io.temporal.workflow.Workflow;

/**
 * Define the parent workflow implementation. It implements the getGreeting workflow method
 *
 * <p>Note that a workflow implementation must always be public for the Temporal library to be able
 * to create its instances.
 */
public class ChildWorkflowImpl implements ChildWorkflow {

  @Override
  public String composeGreeting(String greeting, String name) {

    // Sleep for 2 seconds to ensure the child completes after the parent.
    Workflow.sleep(2000);

    return greeting + " " + name + "!";
  }
}
