package io.temporal.samples.workflowstreams;

import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.samples.workflowstreams.Shared.HubInput;
import io.temporal.samples.workflowstreams.Shared.OrderInput;
import io.temporal.samples.workflowstreams.Shared.PipelineInput;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Workflow-side tests. The client subscribe path needs a live Temporal service, so it is exercised
 * by running the scenarios against a dev server (see README).
 */
public class WorkflowStreamsSampleTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(
              OrderWorkflowImpl.class, PipelineWorkflowImpl.class, HubWorkflowImpl.class)
          .setActivityImplementations(new PaymentActivitiesImpl())
          .build();

  private <T> T newWorkflowStub(Class<T> type) {
    return testWorkflowRule
        .getTestEnvironment()
        .getWorkflowClient()
        .newWorkflowStub(
            type,
            WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
  }

  @Test
  public void testOrderWorkflow() {
    OrderWorkflow workflow = newWorkflowStub(OrderWorkflow.class);
    String result = workflow.processOrder(new OrderInput("order-42"));
    assertEquals("charge-order-42", result);
  }

  @Test
  public void testPipelineWorkflow() {
    PipelineWorkflow workflow = newWorkflowStub(PipelineWorkflow.class);
    String result = workflow.runPipeline(new PipelineInput("p1"));
    assertEquals("pipeline p1 done", result);
  }

  @Test
  public void testHubWorkflow() {
    HubWorkflow workflow = newWorkflowStub(HubWorkflow.class);
    WorkflowClient.start(workflow::host, new HubInput("newsroom"));
    workflow.close();
    String result = WorkflowStub.fromTyped(workflow).getResult(String.class);
    assertEquals("hub newsroom closed", result);
  }
}
