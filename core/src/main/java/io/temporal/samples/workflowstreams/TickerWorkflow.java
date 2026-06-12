package io.temporal.samples.workflowstreams;

import io.temporal.samples.workflowstreams.Shared.TickerInput;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Scenario 4: publishes a long run of tick events and bounds the log by periodically truncating
 * everything but the most recent entries. Fast subscribers see every tick; subscribers that fall
 * behind the truncation point silently jump forward to the new base offset.
 */
@WorkflowInterface
public interface TickerWorkflow {
  @WorkflowMethod
  String tick(TickerInput input);
}
