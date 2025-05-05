package io.temporal.samples.peractivityoptions;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableMap;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.RetryOptions;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkflowImplementationOptions;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;

public class PerActivityOptionsTest {
  WorkflowImplementationOptions options =
      WorkflowImplementationOptions.newBuilder()
          .setActivityOptions(
              ImmutableMap.of(
                  "ActivityTypeA",
                  ActivityOptions.newBuilder()
                      .setScheduleToCloseTimeout(Duration.ofSeconds(5))
                      .build(),
                  "ActivityTypeB",
                  ActivityOptions.newBuilder()
                      .setStartToCloseTimeout(Duration.ofSeconds(2))
                      .setRetryOptions(
                          RetryOptions.newBuilder()
                              .setDoNotRetry(NullPointerException.class.getName())
                              .build())
                      .build()))
          .build();

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(options, PerActivityOptionsWorkflowImpl.class)
          .setActivityImplementations(new FailingActivitiesImpl())
          .build();

  @Test
  public void testPerActivityTypeWorkflow() {
    PerActivityOptionsWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                PerActivityOptionsWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    WorkflowStub untyped = WorkflowStub.fromTyped(workflow);
    WorkflowExecution execution = untyped.start();
    // wait until workflow completes
    untyped.getResult(Void.class);

    DescribeWorkflowExecutionResponse resp =
        testWorkflowRule
            .getWorkflowClient()
            .getWorkflowServiceStubs()
            .blockingStub()
            .describeWorkflowExecution(
                DescribeWorkflowExecutionRequest.newBuilder()
                    .setNamespace(testWorkflowRule.getTestEnvironment().getNamespace())
                    .setExecution(execution)
                    .build());

    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        resp.getWorkflowExecutionInfo().getStatus());
  }
}
