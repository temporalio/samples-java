

package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class HelloSideEffectTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloSideEffect.SideEffectWorkflowImpl.class)
          .setActivityImplementations(new HelloSideEffect.SideEffectActivitiesImpl())
          .build();

  @Test
  public void testSideffectsWorkflow() {
    // Get a workflow stub using the same task queue the worker uses.
    HelloSideEffect.SideEffectWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloSideEffect.SideEffectWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    // Execute a workflow waiting for it to complete.
    String result = workflow.execute();
    // make sure the result is same as the query result after workflow completion
    assertEquals(result, workflow.getResult());
  }
}
