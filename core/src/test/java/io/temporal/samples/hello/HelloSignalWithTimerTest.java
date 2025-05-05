

package io.temporal.samples.hello;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class HelloSignalWithTimerTest {
  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloSignalWithTimer.SignalWithTimerWorkflowImpl.class)
          .setActivityImplementations(new HelloSignalWithTimer.ValueProcessingActivitiesImpl())
          .build();

  private static final String WORKFLOW_ID = "SignalWithTimerTestWorkflow";

  @Test
  public void testSignalWithTimer() {
    HelloSignalWithTimer.SignalWithTimerWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloSignalWithTimer.SignalWithTimerWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .setWorkflowId(WORKFLOW_ID)
                    .build());

    WorkflowClient.start(workflow::execute);
    workflow.newValue("1");
    workflow.newValue("2");
    workflow.exit();

    WorkflowStub.fromTyped(workflow).getResult(Void.class);

    DescribeWorkflowExecutionResponse res =
        testWorkflowRule
            .getWorkflowClient()
            .getWorkflowServiceStubs()
            .blockingStub()
            .describeWorkflowExecution(
                DescribeWorkflowExecutionRequest.newBuilder()
                    .setNamespace(testWorkflowRule.getTestEnvironment().getNamespace())
                    .setExecution(WorkflowExecution.newBuilder().setWorkflowId(WORKFLOW_ID).build())
                    .build());

    Assert.assertEquals(
        res.getWorkflowExecutionInfo().getStatus(),
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED);
  }
}
