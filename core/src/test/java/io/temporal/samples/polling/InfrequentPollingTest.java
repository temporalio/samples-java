package io.temporal.samples.polling;

import static org.junit.Assert.*;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.polling.infrequent.InfrequentPollingActivityImpl;
import io.temporal.samples.polling.infrequent.InfrequentPollingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class InfrequentPollingTest {
  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(InfrequentPollingWorkflowImpl.class)
          .setActivityImplementations(new InfrequentPollingActivityImpl(new TestService()))
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
