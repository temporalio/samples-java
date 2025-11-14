package io.temporal.samples.workerversioning;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.VersioningBehavior;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowVersioningBehavior;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

/**
 * This workflow represents one that likely has a short lifetime, and we want to always stay pinned
 * to the same version it began on. Note that generally you won't want or need to include a version
 * number in your workflow name if you're using the worker versioning feature. This sample does it
 * to illustrate changes to the same code over time - but really what we're demonstrating here is
 * the evolution of what would have been one workflow definition.
 */
public class PinnedWorkflowV1Impl implements PinnedWorkflow {

  private static final Logger logger = Workflow.getLogger(PinnedWorkflowV1Impl.class);

  private final List<String> signals = new ArrayList<>();
  private final Activities activities =
      Workflow.newActivityStub(
          Activities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  @WorkflowVersioningBehavior(VersioningBehavior.PINNED)
  public void run() {
    logger.info("Pinned Workflow v1 started. StartTime: {}", Workflow.currentTimeMillis());

    while (true) {
      Workflow.await(() -> !signals.isEmpty());
      String signal = signals.remove(0);
      if ("conclude".equals(signal)) {
        break;
      }
    }

    activities.someActivity("Pinned-v1");
  }

  @Override
  public void doNextSignal(String signal) {
    signals.add(signal);
  }
}
