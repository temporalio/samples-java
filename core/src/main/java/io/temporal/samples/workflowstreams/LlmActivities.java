package io.temporal.samples.workflowstreams;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.samples.workflowstreams.Shared.LlmInput;

@ActivityInterface
public interface LlmActivities {
  /**
   * Streams an LLM completion to the parent workflow's stream and returns the accumulated full
   * text.
   */
  @ActivityMethod
  String streamCompletion(LlmInput input);
}
