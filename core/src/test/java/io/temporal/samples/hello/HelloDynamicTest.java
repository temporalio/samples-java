package io.temporal.samples.hello;

import static org.junit.Assert.*;

import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowUpdateException;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloDynamic}. Doesn't use an external Temporal service. */
public class HelloDynamicTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloDynamic.DynamicGreetingWorkflowImpl.class)
          .setActivityImplementations(new HelloDynamic.DynamicGreetingActivityImpl())
          .build();

  @Test
  public void testActivityImpl() {

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowId(HelloDynamic.WORKFLOW_ID)
            .build();

    WorkflowStub workflow =
        testWorkflowRule.getWorkflowClient().newUntypedWorkflowStub("DynamicWF", workflowOptions);

    // Start execution
    workflow.start(new Object[] {"Hello"});
    // Send signal to execution with first name
    workflow.signal("greetingSignal", new Object[] {"John"});
    // Send invalid name via update
    WorkflowUpdateException workflowUpdateException =
        Assert.assertThrows(
            WorkflowUpdateException.class,
            () -> workflow.update("greetingUpdate", String.class, new Object[] {"Invalid Name"}));
    // Send valid name via update
    workflow.update("greetingUpdate", Object.class, new Object[] {"Mary"});

    // Wait for workflow to finish getting the results
    String result = workflow.getResult(String.class);

    assertNotNull(result);
    assertEquals(
        "DynamicACT: Hello John from: DynamicWF\nDynamicACT: Hello Mary from: DynamicWF\n", result);
  }
}
