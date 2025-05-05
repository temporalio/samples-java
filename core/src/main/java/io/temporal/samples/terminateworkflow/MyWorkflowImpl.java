

package io.temporal.samples.terminateworkflow;

import io.temporal.workflow.Workflow;
import java.time.Duration;

public class MyWorkflowImpl implements MyWorkflow {
  @Override
  public String execute() {
    // This workflow just sleeps
    Workflow.sleep(Duration.ofSeconds(20));
    return "done";
  }
}
