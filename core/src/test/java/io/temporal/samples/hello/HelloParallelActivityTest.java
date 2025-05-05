package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloParallelActivity}. Doesn't use an external Temporal service. */
public class HelloParallelActivityTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloParallelActivity.MultiGreetingWorkflowImpl.class)
          .setActivityImplementations(new HelloParallelActivity.GreetingActivitiesImpl())
          .build();

  @Test
  public void testParallelActivity() {
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();

    HelloParallelActivity.MultiGreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(HelloParallelActivity.MultiGreetingWorkflow.class, workflowOptions);
    // Execute a workflow waiting for it to complete.
    List<String> results =
        workflow.getGreetings(Arrays.asList("John", "Marry", "Michael", "Janet"));
    assertEquals(4, results.size());
  }
}
