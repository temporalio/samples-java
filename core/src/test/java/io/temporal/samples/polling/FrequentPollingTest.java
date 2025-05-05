package io.temporal.samples.polling;

import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.polling.frequent.FrequentPollingActivityImpl;
import io.temporal.samples.polling.frequent.FrequentPollingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class FrequentPollingTest {
  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(FrequentPollingWorkflowImpl.class)
          .setActivityImplementations(new FrequentPollingActivityImpl(new TestService()))
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
