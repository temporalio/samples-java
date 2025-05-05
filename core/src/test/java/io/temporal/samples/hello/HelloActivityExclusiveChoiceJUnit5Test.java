package io.temporal.samples.hello;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.testing.TestWorkflowExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Unit test for {@link HelloActivityExclusiveChoice}. Doesn't use an external Temporal service. */
public class HelloActivityExclusiveChoiceJUnit5Test {

  @RegisterExtension
  public static final TestWorkflowExtension testWorkflowExtension =
      TestWorkflowExtension.newBuilder()
          .setWorkflowTypes(HelloActivityExclusiveChoice.PurchaseFruitsWorkflowImpl.class)
          .setActivityImplementations(new HelloActivityExclusiveChoice.OrderFruitsActivitiesImpl())
          .build();

  @Test
  public void testWorkflow(HelloActivityExclusiveChoice.PurchaseFruitsWorkflow workflow) {
    // Execute a workflow waiting for it to complete.
    HelloActivityExclusiveChoice.ShoppingList shoppingList =
        new HelloActivityExclusiveChoice.ShoppingList();
    shoppingList.addFruitOrder(HelloActivityExclusiveChoice.Fruits.APPLE, 10);
    StringBuilder orderResults = workflow.orderFruit(shoppingList);
    assertEquals("Ordered 10 Apples...", orderResults.toString());
  }
}
