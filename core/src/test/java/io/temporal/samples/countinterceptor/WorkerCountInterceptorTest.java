package io.temporal.samples.countinterceptor;

import static org.junit.Assert.*;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.samples.countinterceptor.activities.MyActivitiesImpl;
import io.temporal.samples.countinterceptor.workflow.MyChildWorkflowImpl;
import io.temporal.samples.countinterceptor.workflow.MyWorkflow;
import io.temporal.samples.countinterceptor.workflow.MyWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkerFactoryOptions;
import org.junit.Rule;
import org.junit.Test;

public class WorkerCountInterceptorTest {

  private static final String WORKFLOW_ID = "TestInterceptorWorkflow";
  private static final String CHILD_WORKFLOW_ID = "TestInterceptorChildWorkflow";

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(MyWorkflowImpl.class, MyChildWorkflowImpl.class)
          .setActivityImplementations(new MyActivitiesImpl())
          .setWorkerFactoryOptions(
              WorkerFactoryOptions.newBuilder()
                  .setWorkerInterceptors(new SimpleCountWorkerInterceptor())
                  .build())
          .build();

  @Test
  public void testInterceptor() {
    MyWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
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

    assertEquals(1, WorkerCounter.getNumOfWorkflowExecutions(WORKFLOW_ID));
    assertEquals(1, WorkerCounter.getNumOfChildWorkflowExecutions(WORKFLOW_ID));
    // parent workflow does not execute any activities
    assertEquals(0, WorkerCounter.getNumOfActivityExecutions(WORKFLOW_ID));
    // child workflow executes 2 activities
    assertEquals(2, WorkerCounter.getNumOfActivityExecutions(CHILD_WORKFLOW_ID));
    assertEquals(2, WorkerCounter.getNumOfSignals(WORKFLOW_ID));
    assertEquals(2, WorkerCounter.getNumOfQueries(WORKFLOW_ID));
  }
}
