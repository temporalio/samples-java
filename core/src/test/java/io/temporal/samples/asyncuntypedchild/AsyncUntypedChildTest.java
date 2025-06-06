package io.temporal.samples.asyncuntypedchild;

import static io.temporal.api.enums.v1.WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link ParentWorkflowImpl}. Doesn't use an external Temporal service. */
public class AsyncUntypedChildTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder().setDoNotStart(true).build();

  @Test
  public void testMockedChild() {
    testWorkflowRule.getWorker().registerWorkflowImplementationTypes(ParentWorkflowImpl.class);

    // Factory is called to create a new workflow object on each workflow task.
    testWorkflowRule
        .getWorker()
        .registerWorkflowImplementationFactory(
            ChildWorkflow.class,
            () -> {
              ChildWorkflow child = mock(ChildWorkflow.class);
              when(child.composeGreeting("Hello", "World"))
                  .thenReturn("Hello World from mocked child!");
              return child;
            });

    testWorkflowRule.getTestEnvironment().start();

    // Get a workflow stub using the same task queue the worker uses.
    WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();
    ParentWorkflow workflow =
        workflowClient.newWorkflowStub(
            ParentWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    // Execute the parent workflow and wait for it to complete.
    String childWorkflowId = workflow.getGreeting("World");
    assertNotNull(childWorkflowId);

    assertEquals(
        WORKFLOW_EXECUTION_STATUS_RUNNING,
        getChildWorkflowExecutionStatus(workflowClient, childWorkflowId));

    // Wait for the child to complete
    String childResult =
        workflowClient.newUntypedWorkflowStub(childWorkflowId).getResult(String.class);
    assertEquals("Hello World from mocked child!", childResult);

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @NotNull
  private WorkflowExecutionStatus getChildWorkflowExecutionStatus(
      WorkflowClient workflowClient, String childWorkflowId) {
    return workflowClient
        .getWorkflowServiceStubs()
        .blockingStub()
        .describeWorkflowExecution(
            DescribeWorkflowExecutionRequest.newBuilder()
                .setNamespace(testWorkflowRule.getTestEnvironment().getNamespace())
                .setExecution(WorkflowExecution.newBuilder().setWorkflowId(childWorkflowId).build())
                .build())
        .getWorkflowExecutionInfo()
        .getStatus();
  }
}
