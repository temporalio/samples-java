

package io.temporal.samples.getresultsasync;

import io.temporal.workflow.Workflow;
import java.time.Duration;

public class MyWorkflowImpl implements MyWorkflow {
  @Override
  public String justSleep(int seconds) {
    Workflow.sleep(Duration.ofSeconds(seconds));
    return "woke up after " + seconds + " seconds";
  }
}
