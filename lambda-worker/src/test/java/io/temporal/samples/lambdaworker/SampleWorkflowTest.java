package io.temporal.samples.lambdaworker;

import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for the sample Workflow and Activity. */
public class SampleWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(SampleWorkflowImpl.class)
          .setActivityImplementations(new GreetingActivitiesImpl())
          .build();

  @Test
  public void workflowReturnsGreeting() {
    SampleWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                SampleWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    assertEquals(
        "Hello, Serverless Lambda Worker!!", workflow.getGreeting("Serverless Lambda Worker!"));
  }
}
