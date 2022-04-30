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
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.SignalMethod;
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

    /** Users will invoke this signal when they want the main workflow loop to complete. */
    @SignalMethod
    void requestExit();
  }

  /**
   * This is the Activity Definition's Interface. Activities are building blocks of any Temporal
   * Workflow and contain any business logic that could perform long-running computation, network
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

    // In this example, we use an internal of 5 seconds with an intended variation of +/- 1 second
    // between executions of some useful work. In real applications a higher value may be more
    // appropriate, for example one that matches a business cycle of several hours or even days.
    private static final int SCHEDULE_PERIOD_TARGET_SECS = 5;
    private static final int SCHEDULE_PERIOD_VARIATION_SECS = 2;

    // The max history length of a single Temporal workflow is 50,000 commands.
    // Therefore, a workflow cannot run indefinitely. Instead, we use
    // the ContinueAsNew feature to continue the execution as a new run
    // (same approach is used by Temporal's Cron-style scheduling system as well).
    // In real life, the complexity of the workflow affects when we need to flow
    // to a new run: I.e. if the workflow uses more Temporal commands per
    // iteration, then the history grows faster, and thus we can perform fewer
    // iterations before we must ContinueAsNew. For a simple workflow such as
    // this, we could perform many thousands of iterations. However, for
    // demonstration purposes we will flow to a new run more frequently.
    // More details: https://docs.temporal.io/docs/java/workflows/#large-event-histories
    private static final int SINGLE_WORKFLOW_ITERATIONS = 10;

    // Here we introduce a random delay between periodic executions.
    // Note that inside of workflow code 'Workflow.newRandom()' must always
    // be used to construct instances of the 'Random' class.
    private final Random random = Workflow.newRandom();

    /**
     * Define the GreetingActivities stub. Activity stubs are proxies for activity invocations that
     * are executed outside the workflow thread on the activity worker, that can be on a different
     * host. Temporal is going to dispatch the activity results back to the workflow and unblock the
     * stub as soon as activity is completed on the activity worker.
     *
     * <p>In the {@link ActivityOptions} definition the "setStartToCloseTimeout" option sets the
     * maximum time of a single Activity execution attempt. For this example it is set to 10
     * seconds.
     */
    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    private boolean exitRequested = false;

    @Override
    public void requestExit() {
      exitRequested = true;
    }

    @Override
    public void greetPeriodically(String name) {

      for (int i = 0; i < SINGLE_WORKFLOW_ITERATIONS; i++) {

        // Compute the timing of the next iteration:
        int delayMillis =
            (SCHEDULE_PERIOD_TARGET_SECS * 1000)
                + random.nextInt(SCHEDULE_PERIOD_VARIATION_SECS * 1000)
                - (SCHEDULE_PERIOD_VARIATION_SECS * 500);

        // Perform some useful work. In this example, we execute a greeting activity:
        activities.greet(
            "Hello "
                + name
                + "!"
                + " I will sleep for "
                + delayMillis
                + " milliseconds and then I will greet you again.");

        // Sleep for the required time period or until an exit signal is received:
        Workflow.await(Duration.ofMillis(delayMillis), () -> exitRequested);

        if (exitRequested) {
          activities.greet(
              "Hello "
                  + name
                  + "!"
                  + " It was requested to quit the periodic greetings, so this the last one.");
          return;
        }
      }

      // Create a workflow stub that will be used to continue this workflow as a new run:
      // (see the comment for 'SingleWorkflowRunIterations' for details)
      GreetingWorkflow continueAsNew = Workflow.newContinueAsNewStub(GreetingWorkflow.class);

      // Request that the new run will be invoked by the Temporal system:
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
  @SuppressWarnings(
      "CatchAndPrintStackTrace") // in this simple example advanced error logging is not required
  public static void main(String[] args) throws InterruptedException {

    // Define the workflow service.
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
     * Register our workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    /*
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

    // Execute our workflow.
    try {
      WorkflowClient.start(workflow::greetPeriodically, "World");
      System.out.println("Started a new GreetingWorkflow.");
    } catch (WorkflowExecutionAlreadyStarted e) {
      // If the workflow is already running, we cannot start it. Instead, we will connect to the
      // existing instance.
      workflow = client.newWorkflowStub(GreetingWorkflow.class, WORKFLOW_ID);
      System.out.println("Connected to an existing GreetingWorkflow.");
    }

    // The workflow is running now. In this example, we pause for a few seconds to observe its
    // output.
    final int ObservationPeriodSecs = 20;
    System.out.println(
        "Observing the workflow execution for " + ObservationPeriodSecs + " seconds.");

    try {
      Thread.sleep(ObservationPeriodSecs * 1000);
    } catch (InterruptedException intEx) {
      intEx.printStackTrace();
    }

    // Signal the workflow to exit.
    System.out.println("Requesting the workflow to exit.");
    workflow.requestExit();

    // Allow the workflow to finish before the worker is shut down.
    WorkflowStub.fromTyped(workflow).getResult(void.class);

    System.out.println("Good bye.");
    System.exit(0);
  }
}
