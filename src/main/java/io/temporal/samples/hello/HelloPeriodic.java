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

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.Random;

/**
 * Sample Temporal workflow that demonstrates periodic workflow execution with a random delay. To
 * learn about periodic execution with a fixed delay (defined by a cron), check out the {@link
 * HelloCron} example.
 */
public class HelloPeriodic {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloPeriodicTaskQueue";

  // Define the workflow unique id
  static final String WORKFLOW_ID = "HelloPeriodicWorkflow";

  /**
   * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
   *
   * <p>Workflow Definitions should not contain any heavyweight computations, non-deterministic
   * code, network calls, database operations, etc. Those things should be handled by the
   * Activities.
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface GreetingWorkflow {

    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    void greetPeriodically(String name);
  }

  /**
   * This is the Activity Definition's Interface. Activities are building blocks of any Temporal
   * Workflow and contain any business logic that could perform long running computation, network
   * calls, etc.
   *
   * <p>Annotating Activity Definition methods with @ActivityMethod is optional.
   *
   * @see io.temporal.activity.ActivityInterface
   * @see io.temporal.activity.ActivityMethod
   */
  @ActivityInterface
  public interface GreetingActivities {

    // Define your activity method which can be called during workflow execution
    void greet(String greeting);
  }

  // Define the workflow implementation which implements the greetPeriodically workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    // Here we introduce a random delay between periodic executions
    private final Random random = Workflow.newRandom();

    /**
     * Define the GreetingActivities stub. Activity stubs are proxies for activity invocations that
     * are executed outside of the workflow thread on the activity worker, that can be on a
     * different host. Temporal is going to dispatch the activity results back to the workflow and
     * unblock the stub as soon as activity is completed on the activity worker.
     *
     * <p>In the {@link ActivityOptions} definition the "setStartToCloseTimeout" option sets the
     * maximum time of a single Activity execution attempt. For this example it is set to 10
     * seconds.
     */
    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    // Create our workflow stub that can be used to continue this workflow as a new run
    // We use this because we want each workflow execution to be a separate workflow invocation
    private final GreetingWorkflow continueAsNew =
        Workflow.newContinueAsNewStub(GreetingWorkflow.class);

    @Override
    public void greetPeriodically(String name) {

      // Loop as many times as defined in CONTINUE_AS_NEW_FREQUENCY
      /*
       * Here we define how many times we want to execute our workflow activity periodically.
       * The value is set to 10 for the sake of the example. In real applications it would make
       * more sense to set this to a higher value that matches a business cycle (for example once
       * per 24 hours, etc).
       */
      int CONTINUE_AS_NEW_FREQUENCY = 10;

      for (int i = 0; i < CONTINUE_AS_NEW_FREQUENCY; i++) {
        // execute our activity method and sleep for a random amount of time
        int delayMillis = random.nextInt(10000);
        activities.greet("Hello " + name + "! Sleeping for " + delayMillis + " milliseconds.");
        Workflow.sleep(delayMillis);
      }
      // Stop execution of the current workflow and start a new execution
      continueAsNew.greetPeriodically(name);
    }
  }

  /**
   * Implementation of our workflow activity interface. It overwrites our defined greet activity
   * method.
   */
  static class GreetingActivitiesImpl implements GreetingActivities {
    @Override
    public void greet(String greeting) {
      System.out.println(
          "From " + Activity.getExecutionContext().getInfo().getWorkflowId() + ": " + greeting);
    }
  }

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) throws InterruptedException {

    // Define the workflow service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();

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
     * Register our workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    /**
     * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    // Create the workflow client stub. It is used to start our workflow execution.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class,
            // At most one instance.
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    try {
      // execute our workflow
      WorkflowClient.start(workflow::greetPeriodically, "World");
    } catch (WorkflowExecutionAlreadyStarted e) {
      System.out.println("Started: " + e.getMessage());
    }
  }
}
