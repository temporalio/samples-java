package io.temporal.samples.workflowstreams;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.samples.workflowstreams.Shared.LlmInput;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflowstreams.WorkflowStream;
import java.time.Duration;

public class LlmWorkflowImpl implements LlmWorkflow {

  private final LlmActivities activities =
      Workflow.newActivityStub(
          LlmActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofMinutes(2))
              .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
              .build());

  /**
   * Construct the stream from a {@code @WorkflowInit} constructor so the publish-signal handler is
   * registered before any external publisher (the activity, here) tries to publish.
   */
  @WorkflowInit
  public LlmWorkflowImpl(LlmInput input) {
    WorkflowStream.newInstance(input.streamState);
  }

  @Override
  public String complete(LlmInput input) {
    String result = activities.streamCompletion(input);

    // Hold the run open briefly so the consumer's next poll delivers the activity's
    // terminal "complete" event before the workflow exits and the in-memory log is gone.
    Workflow.sleep(OrderWorkflowImpl.DRAIN_DELAY);
    return result;
  }
}
