package io.temporal.samples.hello;

import static org.hamcrest.MatcherAssert.assertThat;

import io.temporal.testing.WorkflowReplayer;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for replay {@link HelloActivity.GreetingWorkflowImpl}. Doesn't use an external Temporal
 * service.
 */
public class HelloActivityReplayTest {

  @Test
  public void replayWorkflowExecutionFromResource() throws Exception {
    WorkflowReplayer.replayWorkflowExecutionFromResource(
        "hello_activity_replay.json", HelloActivity.GreetingWorkflowImpl.class);
  }

  @Test
  public void replayWorkflowExecutionFromResourceNonDeterministic() {

    // hello_activity_replay_non_deterministic.json Event History is the result of executing a
    // different
    // workflow implementation than HelloActivity.GreetingWorkflowImpl.class therefore we expect an
    // exception during the replay
    try {
      WorkflowReplayer.replayWorkflowExecutionFromResource(
          "hello_activity_replay_non_deterministic.json", HelloActivity.GreetingWorkflowImpl.class);

      Assert.fail("Should have thrown an Exception");
    } catch (Exception e) {
      assertThat(
          e.getMessage(),
          CoreMatchers.containsString("error=io.temporal.worker.NonDeterministicException"));
    }
  }
}
