

package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;

import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello.HelloAsyncActivityCompletion.GreetingActivitiesImpl;
import io.temporal.samples.hello.HelloAsyncActivityCompletion.GreetingWorkflow;
import io.temporal.samples.hello.HelloAsyncActivityCompletion.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloAsyncActivityCompletion}. Doesn't use an external Temporal service. */
public class HelloAsyncActivityCompletionTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(GreetingWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testActivityImpl() throws ExecutionException, InterruptedException {
    ActivityCompletionClient completionClient =
        testWorkflowRule.getWorkflowClient().newActivityCompletionClient();
    testWorkflowRule
        .getWorker()
        .registerActivitiesImplementations(new GreetingActivitiesImpl(completionClient));
    testWorkflowRule.getTestEnvironment().start();

    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    // Execute a workflow asynchronously.
    CompletableFuture<String> greeting = WorkflowClient.execute(workflow::getGreeting, "World");
    // Wait for workflow completion.
    assertEquals("Hello World!", greeting.get());

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
