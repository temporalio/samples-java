package io.temporal.samples.retryonsignalinterceptor;

import static org.junit.Assert.*;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkerFactoryOptions;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;

public class RetryOnSignalInterceptorTest {

  static class TestActivityImpl implements MyActivity {

    final AtomicInteger count = new AtomicInteger();

    @Override
    public void execute() {
      if (count.incrementAndGet() < 5) {
        throw ApplicationFailure.newFailure("simulated", "type1");
      }
    }
  }

  private final TestActivityImpl testActivity = new TestActivityImpl();

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkerFactoryOptions(
              WorkerFactoryOptions.newBuilder()
                  .setWorkerInterceptors(new RetryOnSignalWorkerInterceptor())
                  .validateAndBuildWithDefaults())
          .setWorkflowTypes(MyWorkflowImpl.class)
          .setActivityImplementations(testActivity)
          .build();

  @Test
  public void testRetryThenFail() {
    testActivity.count.set(0);
    TestWorkflowEnvironment testEnvironment = testWorkflowRule.getTestEnvironment();
    MyWorkflow workflow = testWorkflowRule.newWorkflowStub(MyWorkflow.class);
    WorkflowExecution execution = WorkflowClient.start(workflow::execute);

    // Get stub to the dynamically registered interface
    RetryOnSignalInterceptorListener listener =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(RetryOnSignalInterceptorListener.class, execution.getWorkflowId());
    testEnvironment.sleep(Duration.ofMinutes(10));
    listener.retry();
    testEnvironment.sleep(Duration.ofMinutes(10));
    listener.retry();
    testEnvironment.sleep(Duration.ofMinutes(10));
    listener.retry();
    testEnvironment.sleep(Duration.ofMinutes(10));
    listener.fail();
    WorkflowStub untyped =
        testWorkflowRule.getWorkflowClient().newUntypedWorkflowStub(execution.getWorkflowId());
    try {
      untyped.getResult(Void.class);
      fail("unreachable");
    } catch (Exception e) {
      assertTrue(e.getCause() instanceof ActivityFailure);
      assertTrue(e.getCause().getCause() instanceof ApplicationFailure);
      assertEquals(
          "message='simulated', type='type1', nonRetryable=false",
          e.getCause().getCause().getMessage());
    }
    assertEquals(4, testActivity.count.get());
  }

  @Test
  public void testRetryUntilSucceeds() {
    testActivity.count.set(0);
    TestWorkflowEnvironment testEnvironment = testWorkflowRule.getTestEnvironment();
    MyWorkflow workflow = testWorkflowRule.newWorkflowStub(MyWorkflow.class);
    WorkflowExecution execution = WorkflowClient.start(workflow::execute);

    // Get stub to the dynamically registered interface
    RetryOnSignalInterceptorListener listener =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(RetryOnSignalInterceptorListener.class, execution.getWorkflowId());
    for (int i = 0; i < 4; i++) {
      testEnvironment.sleep(Duration.ofMinutes(10));
      listener.retry();
    }
    WorkflowStub untyped =
        testWorkflowRule.getWorkflowClient().newUntypedWorkflowStub(execution.getWorkflowId());
    untyped.getResult(Void.class);
    assertEquals(5, testActivity.count.get());
  }
}
