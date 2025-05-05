

package io.temporal.samples.hello;

import static io.temporal.samples.hello.HelloActivityExclusiveChoice.WORKFLOW_ID;
import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloActivityExclusiveChoice}. Doesn't use an external Temporal service. */
public class HelloActivityExclusiveChoiceTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloActivityExclusiveChoice.PurchaseFruitsWorkflowImpl.class)
          .setActivityImplementations(new HelloActivityExclusiveChoice.OrderFruitsActivitiesImpl())
          .build();

  @Test
  public void testWorkflow() {
    // Get a workflow stub using the same task queue the worker uses.
    HelloActivityExclusiveChoice.PurchaseFruitsWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloActivityExclusiveChoice.PurchaseFruitsWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId(WORKFLOW_ID)
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .build());
    // Execute a workflow waiting for it to complete.
    HelloActivityExclusiveChoice.ShoppingList shoppingList =
        new HelloActivityExclusiveChoice.ShoppingList();
    shoppingList.addFruitOrder(HelloActivityExclusiveChoice.Fruits.APPLE, 10);
    StringBuilder orderResults = workflow.orderFruit(shoppingList);
    assertEquals("Ordered 10 Apples...", orderResults.toString());
  }
}
