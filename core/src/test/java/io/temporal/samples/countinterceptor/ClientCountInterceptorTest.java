package io.temporal.samples.countinterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.samples.countinterceptor.activities.MyActivitiesImpl;
import io.temporal.samples.countinterceptor.workflow.MyChildWorkflowImpl;
import io.temporal.samples.countinterceptor.workflow.MyWorkflow;
import io.temporal.samples.countinterceptor.workflow.MyWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class ClientCountInterceptorTest {

  private static final String WORKFLOW_ID = "TestInterceptorWorkflow";

  private final ClientCounter clientCounter = new ClientCounter();

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(MyWorkflowImpl.class, MyChildWorkflowImpl.class)
          .setActivityImplementations(new MyActivitiesImpl())
          .setWorkflowClientOptions(
              WorkflowClientOptions.newBuilder()
                  .setInterceptors(new SimpleClientInterceptor(clientCounter))
                  .build())
          .build();

  @Test
  public void testInterceptor() {
    WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();

    MyWorkflow workflow =
        workflowClient.newWorkflowStub(
            MyWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(testWorkflowRule.getTaskQueue())
                .setWorkflowId(WORKFLOW_ID)
                .build());

    WorkflowClient.start(workflow::exec);

    workflow.signalNameAndTitle("John", "Customer");

    String name = workflow.queryName();
    String title = workflow.queryTitle();

    workflow.exit();

    // Wait for workflow completion via WorkflowStub
    WorkflowStub untyped = WorkflowStub.fromTyped(workflow);
    String result = untyped.getResult(String.class);

    assertNotNull(result);

    assertNotNull(name);
    assertEquals("John", name);
    assertNotNull(title);
    assertEquals("Customer", title);

    assertEquals(1, clientCounter.getNumOfWorkflowExecutions(WORKFLOW_ID));
    assertEquals(1, clientCounter.getNumOfGetResults(WORKFLOW_ID));
    assertEquals(2, clientCounter.getNumOfSignals(WORKFLOW_ID));
    assertEquals(2, clientCounter.getNumOfQueries(WORKFLOW_ID));
  }
}
