package io.temporal.samples.springboot.customize;

import io.temporal.activity.ActivityOptions;
import io.temporal.activity.LocalActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.TimeoutFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;

/**
 * In our custom config we have set that worker polling on CustomizeTaskQueue to be a "local
 * activity worker", meaning it would not poll for activity tasks. For this sample we will try to
 * start an activity as "normal" activity which should time out, then invoke it again as local which
 * should be successful.
 *
 * @see io.temporal.samples.springboot.customize.TemporalOptionsConfig
 */
@WorkflowImpl(taskQueues = "CustomizeTaskQueue")
public class CustomizeWorkflowImpl implements CustomizeWorkflow {
  private CustomizeActivity asNormalActivity =
      Workflow.newActivityStub(
          CustomizeActivity.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(2))
              .setScheduleToCloseTimeout(Duration.ofSeconds(4))
              .build());

  private CustomizeActivity asLocalActivity =
      Workflow.newLocalActivityStub(
          CustomizeActivity.class,
          LocalActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());
  private Logger logger = Workflow.getLogger(CustomizeActivity.class.getName());

  @Override
  public String execute() {
    try {
      return asNormalActivity.run("Normal");
    } catch (ActivityFailure e) {
      // We should have TimeoutFailure as activity failure cause with StartToClose timeout type
      TimeoutFailure tf = (TimeoutFailure) e.getCause();
      logger.warn("asNormalActivity failed with timeout type: " + tf.getTimeoutType());
    }
    return asLocalActivity.run("Local");
  }
}
