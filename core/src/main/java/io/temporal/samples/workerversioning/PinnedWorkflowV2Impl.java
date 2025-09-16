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
 * This workflow has changes that would make it incompatible with v1, and aren't protected by a
 * patch.
 */
public class PinnedWorkflowV2Impl implements PinnedWorkflow {

  private static final Logger logger = Workflow.getLogger(PinnedWorkflowV2Impl.class);

  private final List<String> signals = new ArrayList<>();
  private final Activities activities =
      Workflow.newActivityStub(
          Activities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  @WorkflowVersioningBehavior(VersioningBehavior.PINNED)
  public void run() {
    logger.info("Pinned Workflow v2 started. StartTime: {}", Workflow.currentTimeMillis());

    // Here we call an activity where we didn't before, which is an incompatible change.
    activities.someActivity("Pinned-v2");

    while (true) {
      Workflow.await(() -> !signals.isEmpty());
      String signal = signals.remove(0);
      if ("conclude".equals(signal)) {
        break;
      }
    }

    // We've also changed the activity type here, another incompatible change
    activities.someIncompatibleActivity(
        new Activities.IncompatibleActivityInput("Pinned-v2", "hi"));
  }

  @Override
  public void doNextSignal(String signal) {
    signals.add(signal);
  }
}
