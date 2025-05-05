package io.temporal.samples.hello;

import static org.junit.Assert.*;

import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
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

    // Start workflow execution and signal right after Pass in the workflow args and signal args
    workflow.signalWithStart("greetingSignal", new Object[] {"John"}, new Object[] {"Hello"});

    // Wait for workflow to finish getting the results
    String result = workflow.getResult(String.class);

    assertNotNull(result);
    assertEquals("DynamicACT: Hello John from: DynamicWF", result);
  }
}
