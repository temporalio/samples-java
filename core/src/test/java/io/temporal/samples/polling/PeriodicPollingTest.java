

package io.temporal.samples.polling;

import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.polling.periodicsequence.PeriodicPollingActivityImpl;
import io.temporal.samples.polling.periodicsequence.PeriodicPollingChildWorkflowImpl;
import io.temporal.samples.polling.periodicsequence.PeriodicPollingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class PeriodicPollingTest {
  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(
              PeriodicPollingWorkflowImpl.class, PeriodicPollingChildWorkflowImpl.class)
          .setActivityImplementations(new PeriodicPollingActivityImpl(new TestService()))
          .build();

  @Test
  public void testInfrequentPoll() {
    PollingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                PollingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    assertEquals("OK", workflow.exec());
  }
}
