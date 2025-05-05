

package io.temporal.samples.getresultsasync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import java.util.concurrent.CompletableFuture;
import org.junit.Rule;
import org.junit.Test;

public class GetResultsAsyncTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder().setWorkflowTypes(MyWorkflowImpl.class).build();

  @Test
  public void testGetResultsAsync() throws Exception {

    WorkflowStub workflowStub =
        testWorkflowRule
            .getWorkflowClient()
            .newUntypedWorkflowStub(
                "MyWorkflow",
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    workflowStub.start(5);

    CompletableFuture<String> completableFuture = workflowStub.getResultAsync(String.class);

    String result = completableFuture.get();
    assertNotNull(result);
    assertEquals("woke up after 5 seconds", result);
  }
}
