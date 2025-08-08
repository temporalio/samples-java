package io.temporal.samples.hello;

import io.temporal.activity.*;
import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.client.*;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.workflow.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class HelloUpdateAndCancellationTest {
  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(TestWorkflowImpl.class)
          .setActivityImplementations(new TestActivitiesImpl())
          .build();

  @Test
  public void testUpdateAndWorkflowCancellation() {
    // Start workflow with UpdateWithStart then cancel workflow before activity completes
    TestWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                TestWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("test-workflow-cancel")
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .setWorkflowIdConflictPolicy(
                        WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_USE_EXISTING)
                    .build());

    WorkflowUpdateHandle<String> updateHandle =
        WorkflowClient.startUpdateWithStart(
            workflow::mileStoneCompleted,
            UpdateOptions.<String>newBuilder()
                .setWaitForStage(WorkflowUpdateStage.ACCEPTED)
                .build(),
            new WithStartWorkflowOperation<>(workflow::execute));

    testWorkflowRule
        .getTestEnvironment()
        .registerDelayedCallback(
            Duration.ofSeconds(3),
            () -> {
              WorkflowStub.fromTyped(workflow).cancel("canceled by test");
            });

    String updateResult = updateHandle.getResult();
    Assert.assertEquals("milestone canceled", updateResult);

    try {
      WorkflowStub.fromTyped(workflow).getResult(String.class);
      Assert.fail("Workflow Execution should have been canceled");
    } catch (WorkflowFailedException e) {
      // Our workflow should have been canceled
      Assert.assertEquals(CanceledFailure.class, e.getCause().getClass());
    }
  }

  @Test
  public void testUpdateAndActivityCancellation() {
    // Start workflow with UpdateWithStart then cancel the activity only by sending signal to
    // execution
    TestWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                TestWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("test-activity-cancel")
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .setWorkflowIdConflictPolicy(
                        WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_USE_EXISTING)
                    .build());

    WorkflowUpdateHandle<String> updateHandle =
        WorkflowClient.startUpdateWithStart(
            workflow::mileStoneCompleted,
            UpdateOptions.<String>newBuilder()
                .setWaitForStage(WorkflowUpdateStage.ACCEPTED)
                .build(),
            new WithStartWorkflowOperation<>(workflow::execute));

    testWorkflowRule
        .getTestEnvironment()
        .registerDelayedCallback(
            Duration.ofSeconds(3),
            () -> {
              WorkflowStub.fromTyped(workflow).signal("cancelActivity");
            });

    String updateResult = updateHandle.getResult();
    Assert.assertEquals("milestone canceled", updateResult);

    try {
      WorkflowStub.fromTyped(workflow).getResult(String.class);
      Assert.fail("Workflow Execution should have failed");
    } catch (WorkflowFailedException e) {
      // In this case we did not cancel workflow execution but we failed it by throwing
      // ActivityFailure
      Assert.assertEquals(ActivityFailure.class, e.getCause().getClass());
      ActivityFailure af = (ActivityFailure) e.getCause();
      // Since we canceled the activity still, the cause of ActivityFailure should be
      // CanceledFailure
      Assert.assertEquals(CanceledFailure.class, af.getCause().getClass());
    }
  }

  @WorkflowInterface
  public interface TestWorkflow {
    @WorkflowMethod
    String execute();

    @UpdateMethod
    String mileStoneCompleted();

    @SignalMethod
    void cancelActivity();
  }

  public static class TestWorkflowImpl implements TestWorkflow {
    boolean milestoneDone, mileStoneCanceled;
    CancellationScope scope;
    TestActivities activities =
        Workflow.newActivityStub(
            TestActivities.class,
            ActivityOptions.newBuilder()
                .setHeartbeatTimeout(Duration.ofSeconds(3))
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                .build());

    @Override
    public String execute() {
      scope =
          Workflow.newCancellationScope(
              () -> {
                activities.runActivity();
              });

      try {
        scope.run();
        milestoneDone = true;
        Workflow.await(Workflow::isEveryHandlerFinished);
        return "workflow completed";
      } catch (ActivityFailure e) {
        if (e.getCause() instanceof CanceledFailure) {
          CancellationScope detached =
              Workflow.newDetachedCancellationScope(
                  () -> {
                    mileStoneCanceled = true;
                    Workflow.await(Workflow::isEveryHandlerFinished);
                  });
          detached.run();
        }
        throw e;
      }
    }

    @Override
    public String mileStoneCompleted() {
      Workflow.await(() -> milestoneDone || mileStoneCanceled);
      // For sake of testing isEveryHandlerFinished block here for 2 seconds
      Workflow.sleep(Duration.ofSeconds(2));
      return milestoneDone ? "milestone completed" : "milestone canceled";
    }

    @Override
    public void cancelActivity() {
      if (scope != null) {
        scope.cancel("test reason");
      }
    }
  }

  @ActivityInterface
  public interface TestActivities {
    void runActivity();
  }

  public static class TestActivitiesImpl implements TestActivities {

    @Override
    public void runActivity() {
      ActivityExecutionContext context = Activity.getExecutionContext();
      for (int i = 0; i < 9; i++) {
        sleep(1);
        try {
          context.heartbeat(i);
        } catch (ActivityCompletionException e) {
          throw e;
        }
      }
    }
  }

  private static void sleep(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException e) {
    }
  }
}
