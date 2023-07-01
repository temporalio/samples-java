/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.hello;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Sample Temporal Workflow Definition demonstrating how to execute an Activity based on dynamic
 * input.
 */
public class HelloActivityExclusiveChoice {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloActivityChoiceTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloActivityChoiceWorkflow";

  // Different fruits you can order
  enum Fruits {
    APPLE,
    BANANA,
    CHERRY,
    ORANGE
  }

  // Our example shopping list for different fruits
  public static class ShoppingList {
    private Map<Fruits, Integer> list = new HashMap<>();

    public void addFruitOrder(Fruits fruit, int amount) {
      list.put(fruit, amount);
    }

    public Map<Fruits, Integer> getList() {
      return list;
    }
  }

  /**
   * Define the Workflow Interface. It must contain at least one method annotated
   * with @WorkflowMethod
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface PurchaseFruitsWorkflow {

    /**
     * Define the workflow method. This method is executed when the workflow is started. The
     * workflow completes when the workflow method finishes execution.
     */
    @WorkflowMethod
    StringBuilder orderFruit(ShoppingList list);
  }

  /**
   * Define the Activity Interface. Workflow methods can call activities during execution.
   * Annotating activity methods with @ActivityMethod is optional
   *
   * @see io.temporal.activity.ActivityInterface
   * @see io.temporal.activity.ActivityMethod
   */
  @ActivityInterface
  public interface OrderFruitsActivities {
    // Define your activity methods which can be called during workflow execution

    String orderApples(int amount);

    String orderBananas(int amount);

    String orderCherries(int amount);

    String orderOranges(int amount);
  }

  // Define the workflow implementation. It implements our orderFruit workflow method
  public static class PurchaseFruitsWorkflowImpl implements PurchaseFruitsWorkflow {

    /*
     * Define the OrderActivities stub. Activity stubs implements activity interfaces and proxy
     * calls to it to Temporal activity invocations. Since Temporal activities are reentrant, a
     * single activity stub can be used for multiple activity invocations.
     *
     * <p>In the {@link ActivityOptions} definition the "setStartToCloseTimeout" option sets the
     * maximum time of a single Activity execution attempt. For this example it is set to 2 seconds.
     */
    private final OrderFruitsActivities activities =
        Workflow.newActivityStub(
            OrderFruitsActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    @Override
    public StringBuilder orderFruit(ShoppingList list) {
      StringBuilder shoppingResults = new StringBuilder();
      // Go through each element of our shopping list
      list.getList()
          .forEach(
              (fruit, amount) -> {
                // You can use a basic switch to call an activity method based on the workflow input
                switch (fruit) {
                  case APPLE:
                    shoppingResults.append(activities.orderApples(amount));
                    break;
                  case BANANA:
                    shoppingResults.append(activities.orderBananas(amount));
                    break;
                  case CHERRY:
                    shoppingResults.append(activities.orderCherries(amount));
                    break;
                  case ORANGE:
                    shoppingResults.append(activities.orderOranges(amount));
                    break;
                  default:
                    shoppingResults.append("Unable to order fruit: ").append(fruit);
                    break;
                }
              });
      return shoppingResults;
    }
  }

  /**
   * Implementation of our workflow activity interface. It overwrites our defined activity methods.
   */
  static class OrderFruitsActivitiesImpl implements OrderFruitsActivities {
    @Override
    public String orderApples(int amount) {
      return "Ordered " + amount + " Apples...";
    }

    @Override
    public String orderBananas(int amount) {
      return "Ordered " + amount + " Bananas...";
    }

    @Override
    public String orderCherries(int amount) {
      return "Ordered " + amount + " Cherries...";
    }

    @Override
    public String orderOranges(int amount) {
      return "Ordered " + amount + " Oranges...";
    }
  }

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method is our
   * workflow starter.
   */
  public static void main(String[] args) {
    /*
     * Define the workflow service. It is a gRPC stubs wrapper which talks to the docker instance of
     * our locally running Temporal service.
     */
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    /*
     * Get a Workflow service client which can be used to start, Signal, and Query Workflow Executions.
     */
    WorkflowClient client = WorkflowClient.newInstance(service);

    /*
     * Define the workflow factory. It is used to create workflow workers for a specific task queue.
     */
    WorkerFactory factory = WorkerFactory.newInstance(client);

    /*
     * Define the workflow worker. Workflow workers listen to a defined task queue and process
     * workflows and activities.
     */
    Worker worker = factory.newWorker(TASK_QUEUE);

    /*
     * Register our workflow implementation with the worker. Since workflows are stateful in nature,
     * we need to register our workflow type.
     */
    worker.registerWorkflowImplementationTypes(PurchaseFruitsWorkflowImpl.class);

    /*
     * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(new OrderFruitsActivitiesImpl());

    // Start all the workers registered for a specific task queue.
    factory.start();

    // Create our workflow client stub. It is used to start our workflow execution.
    PurchaseFruitsWorkflow workflow =
        client.newWorkflowStub(
            PurchaseFruitsWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    // Let's build our example shopping list
    ShoppingList shoppingList = new ShoppingList();
    shoppingList.addFruitOrder(Fruits.APPLE, 8);
    shoppingList.addFruitOrder(Fruits.BANANA, 5);
    shoppingList.addFruitOrder(Fruits.CHERRY, 1);
    shoppingList.addFruitOrder(Fruits.ORANGE, 4);

    // Execute our workflow method
    StringBuilder orderResults = workflow.orderFruit(shoppingList);

    System.out.println("Order results: " + orderResults);
    System.exit(0);
  }
}
