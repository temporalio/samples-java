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
 * This represents us having made *compatible* changes to AutoUpgradingWorkflowV1Impl.
 *
 * <p>The compatible changes we've made are:
 *
 * <ul>
 *   <li>Altering the log lines
 *   <li>Using the `Workflow.getVersion` API to properly introduce branching behavior while
 *       maintaining compatibility
 * </ul>
 */
public class AutoUpgradingWorkflowV1bImpl implements AutoUpgradingWorkflow {

  private static final Logger logger = Workflow.getLogger(AutoUpgradingWorkflowV1bImpl.class);

  private final List<String> signals = new ArrayList<>();
  private final Activities activities =
      Workflow.newActivityStub(
          Activities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  @WorkflowVersioningBehavior(VersioningBehavior.AUTO_UPGRADE)
  public void run() {
    logger.info("Changing workflow v1b started. StartTime: {}", Workflow.currentTimeMillis());

    while (true) {
      Workflow.await(() -> !signals.isEmpty());
      String signal = signals.remove(0);

      if ("do-activity".equals(signal)) {
        logger.info("Changing workflow v1b running activity");
        int version = Workflow.getVersion("DifferentActivity", Workflow.DEFAULT_VERSION, 1);
        if (version == 1) {
          activities.someIncompatibleActivity(
              new Activities.IncompatibleActivityInput("v1b", "hello!"));
        } else {
          // Note it is a valid compatible change to alter the input to an activity.
          // However, because we're using the getVersion API, this branch will never be
          // taken.
          activities.someActivity("v1b");
        }
      } else {
        logger.info("Concluding workflow v1b");
        break;
      }
    }
  }

  @Override
  public void doNextSignal(String signal) {
    signals.add(signal);
  }
}
