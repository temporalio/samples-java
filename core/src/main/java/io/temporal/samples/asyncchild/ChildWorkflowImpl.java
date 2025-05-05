

package io.temporal.samples.asyncchild;

import io.temporal.workflow.Workflow;
import java.time.Duration;

public class ChildWorkflowImpl implements ChildWorkflow {
  @Override
  public String executeChild() {
    Workflow.sleep(Duration.ofSeconds(3));
    return "Child workflow done";
  }
}
