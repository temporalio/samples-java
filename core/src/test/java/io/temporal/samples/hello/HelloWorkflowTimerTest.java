

package io.temporal.samples.hello;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class HelloWorkflowTimerTest {
  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(
              HelloWorkflowTimer.WorkflowWithTimerImpl.class,
              HelloWorkflowTimer.WorkflowWithTimerChildWorkflowImpl.class)
          .setActivityImplementations(new HelloWorkflowTimer.WorkflowWithTimerActivitiesImpl())
          .build();

  @Test
  public void testWorkflowTimer() {
    HelloWorkflowTimer.WorkflowWithTimer workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloWorkflowTimer.WorkflowWithTimer.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("WorkflowWithTimerTestId")
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .build());

    String result = workflow.execute("test input");
    Assert.assertEquals("Workflow timer fired while activities were executing.", result);
  }
}
