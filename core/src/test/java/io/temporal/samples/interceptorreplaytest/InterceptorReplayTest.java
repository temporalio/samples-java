package io.temporal.samples.interceptorreplaytest;

import static org.junit.Assert.fail;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.common.interceptors.*;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.testing.WorkflowReplayer;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InterceptorReplayTest {
  @RegisterExtension
  public static final TestWorkflowExtension testWorkflowExtension =
      TestWorkflowExtension.newBuilder()
          // Register workflow and activity impls
          .registerWorkflowImplementationTypes(TestWorkflowImpl.class)
          .setActivityImplementations(new TestActivitiesImpl())
          // Register worker interceptor
          .setWorkerFactoryOptions(
              WorkerFactoryOptions.newBuilder()
                  .setWorkerInterceptors(new TestWorkerInterceptor())
                  .build())
          .setDoNotStart(true)
          .build();

  @Test
  // TODO
  public void testReplayWithInterceptors(TestWorkflowEnvironment testEnv, Worker worker) {
    // Run our test workflow. We need to set workflow id so can get history after
    testEnv.start();
    TestWorkflow workflow =
        testEnv
            .getWorkflowClient()
            .newWorkflowStub(
                TestWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("test-workflow")
                    .setTaskQueue(worker.getTaskQueue())
                    .build());
    workflow.execute();

    // Replay execution with history of just executed
    WorkflowExecutionHistory eventHistory =
        testEnv.getWorkflowClient().fetchHistory("test-workflow");

    try {
      WorkflowReplayer.replayWorkflowExecution(eventHistory, worker);
    } catch (Exception e) {
      fail(e.getMessage());
    }
    testEnv.shutdown();

    // Try replaying execution with test env where we dont have interceptors registered
    TestWorkflowEnvironment testEnv2 = TestWorkflowEnvironment.newInstance();
    Worker testEnv2Worker = testEnv2.newWorker("test-taskqueue");
    testEnv2Worker.registerWorkflowImplementationTypes(TestWorkflowImpl.class);
    testEnv2Worker.registerActivitiesImplementations(new TestActivitiesImpl());

    testEnv2.start();

    // Replay should fail with worker that does not have interceptor registered
    try {
      WorkflowReplayer.replayWorkflowExecution(eventHistory, testEnv2Worker);
      fail("This should have failed");
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    // But it should be fine with worker that does
    try {
      WorkflowReplayer.replayWorkflowExecution(eventHistory, worker);
    } catch (Exception e) {
      fail(e.getMessage());
    }

    testEnv2.shutdown();
  }

  // Test workflow and activities
  @WorkflowInterface
  public interface TestWorkflow {
    @WorkflowMethod
    void execute();
  }

  public static class TestWorkflowImpl implements TestWorkflow {

    TestActivities activities =
        Workflow.newActivityStub(
            TestActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    @Override
    public void execute() {
      activities.activityOne();
    }
  }

  @ActivityInterface
  public interface TestActivities {
    void activityOne();

    void activityTwo();

    void activityThree();
  }

  public static class TestActivitiesImpl implements TestActivities {
    @Override
    public void activityOne() {
      System.out.println("Activities one done");
    }

    @Override
    public void activityTwo() {
      System.out.println("Activities two done");
    }

    @Override
    public void activityThree() {
      System.out.println("Activities three done");
    }
  }

  // Test worker and workflow interceptors
  public static class TestWorkerInterceptor extends WorkerInterceptorBase {
    @Override
    public WorkflowInboundCallsInterceptor interceptWorkflow(WorkflowInboundCallsInterceptor next) {
      return new TestWorkflowInboundCallsInterceptor(next);
    }
  }

  public static class TestWorkflowInboundCallsInterceptor
      extends WorkflowInboundCallsInterceptorBase {
    TestActivities activities =
        Workflow.newActivityStub(
            TestActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    public TestWorkflowInboundCallsInterceptor(WorkflowInboundCallsInterceptor next) {
      super(next);
    }

    @Override
    public void init(WorkflowOutboundCallsInterceptor outboundCalls) {
      super.init(new TestWorkflowOutboundCallsInterceptor(outboundCalls));
    }

    @Override
    public WorkflowOutput execute(WorkflowInput input) {
      WorkflowOutput output = super.execute(input);
      // Run activity three before completing execution
      activities.activityThree();
      return output;
    }
  }

  public static class TestWorkflowOutboundCallsInterceptor
      extends WorkflowOutboundCallsInterceptorBase {
    TestActivities activities =
        Workflow.newActivityStub(
            TestActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    public TestWorkflowOutboundCallsInterceptor(WorkflowOutboundCallsInterceptor next) {
      super(next);
    }

    @Override
    public <R> ActivityOutput<R> executeActivity(ActivityInput<R> input) {
      ActivityOutput output = super.executeActivity(input);

      // we only want to intercept ActivityOne here
      if (input.getActivityName().equals("ActivityOne")) {
        activities.activityTwo();
      }

      return output;
    }
  }
}
