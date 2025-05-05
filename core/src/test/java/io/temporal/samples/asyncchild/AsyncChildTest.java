

package io.temporal.samples.asyncchild;

import static org.junit.Assert.assertNotNull;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class AsyncChildTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(ParentWorkflowImpl.class, ChildWorkflowImpl.class)
          .build();

  @Test
  public void testAsyncChildWorkflow() {
    ParentWorkflow parentWorkflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                ParentWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    WorkflowExecution childExecution = parentWorkflow.executeParent();

    assertNotNull(childExecution);
  }
}
