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

import io.temporal.activity.*;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import io.temporal.workflow.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample Temporal Workflow Definition that demonstrates parallel Activity Executions with a
 * Cancellation Scope. When one of the Activity Executions finish, we cancel the execution of the
 * other Activities and wait for their cancellation to complete.
 */
public class HelloCancellationScopeMultithreadHeartbeat {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloCancellationScopeTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloCancellationScopeWorkflow";

  /**
   * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
   *
   * <p>Workflow Definitions should not contain any heavyweight computations, non-deterministic
   * code, network calls, database operations, etc. Those things should be handled by the
   * Activities.
   *
   * @see WorkflowInterface
   * @see WorkflowMethod
   */
  @WorkflowInterface
  public interface GreetingWorkflow {

    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    String getGreeting(String name);
  }

  /**
   * This is the Activity Definition's Interface. Activities are building blocks of any Temporal
   * Workflow and contain any business logic that could perform long running computation, network
   * calls, etc.
   *
   * <p>Annotating Activity Definition methods with @ActivityMethod is optional.
   *
   * @see ActivityInterface
   * @see io.temporal.activity.ActivityMethod
   */
  @ActivityInterface
  public interface GreetingActivities {
    String composeGreeting(String greeting, String name);
  }

  // Define the workflow implementation which implements our getGreeting workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private static final int ACTIVITY_MAX_SLEEP_SECONDS = 30;
    private static final int ACTIVITY_MAX_CLEANUP_SECONDS = 5;
    private static final int ACTIVITY_START_TO_CLOSE_TIMEOUT =
        ACTIVITY_MAX_SLEEP_SECONDS + ACTIVITY_MAX_CLEANUP_SECONDS + 10;

    //    private static final String[] greetings =
    //        new String[] {"Hello", "Bye", "Hola", "Привет", "Oi", "Hallo"};
    private static final String[] greetings =
        new String[] {"Hello", "Bye", "Hola", "Привет", "Oi", "Hallo"};

    /**
     * Define the GreetingActivities stub. Activity stubs are proxies for activity invocations that
     * are executed outside of the workflow thread on the activity worker, that can be on a
     * different host. Temporal is going to dispatch the activity results back to the workflow and
     * unblock the stub as soon as activity is completed on the activity worker.
     *
     * <p>In the {@link ActivityOptions} definition the "setStartToCloseTimeout" option sets the
     * maximum time of a single Activity execution attempt. For this example it is set to 10
     * seconds.
     *
     * <p>The "setCancellationType" option means that in case of activity cancellation the activity
     * should fail with {@link CanceledFailure}. We set
     * ActivityCancellationType.WAIT_CANCELLATION_COMPLETED which denotes that activity should be
     * first notified of the cancellation, and cancelled after it can perform some cleanup tasks for
     * example. Note that an activity must heartbeat to receive cancellation notifications.
     */
    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                // if heartbeat timeout is not set, activity heartbeats will be throttled to one
                // every 30 seconds
                // which is too rare for the cancellations to be delivered in this example.
                .setHeartbeatTimeout(Duration.ofSeconds(5))
                .setStartToCloseTimeout(Duration.ofSeconds(ACTIVITY_START_TO_CLOSE_TIMEOUT))
                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                .build());

    @Override
    public String getGreeting(String name) {
      var logger = Workflow.getLogger(GreetingWorkflowImpl.class);

      List<Promise<String>> results = new ArrayList<>(greetings.length);

      /*
       * Create our CancellationScope. Within this scope we call the workflow activity
       * composeGreeting method asynchronously for each of our defined greetings in different
       * languages.
       */
      CancellationScope scope =
          Workflow.newCancellationScope(
              () -> {
                for (String greeting : greetings) {
                  logger.info("starting greeting {}", greeting);
                  results.add(Async.function(activities::composeGreeting, greeting, name));
                }
              });
      /*
       * Execute all activities within the CancellationScope. Note that this execution is
       * non-blocking as the code inside our cancellation scope is also non-blocking.
       */
      scope.run();
      logger.info("started all the things");

      // We use "anyOf" here to wait for one of the activity invocations to return
      Workflow.newTimer(Duration.ofSeconds(10)).get();
      //      String result = Promise.anyOf(results).get();
      //      logger.info("received all the things {}", result);

      // Trigger cancellation of all uncompleted activity invocations within the cancellation scope
      scope.cancel();

      logger.info("canceled scoped...moving on");
      /*
       *  Wait for all activities to perform cleanup if needed.
       *  For the sake of the example we ignore cancellations and
       *  get all the results so that we can print them in the end.
       *
       *  Note that we cannot use "allOf" here as that fails on any Promise failures
       */
      for (Promise<String> activityResult : results) {
        try {
          activityResult.get();
        } catch (ActivityFailure e) {
          if (!(e.getCause() instanceof CanceledFailure)) {
            throw e;
          }
        }
      }
      return "done";
    }
  }

  static class Greeter implements Callable<String> {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Greeter.class);
    private int mockActivityTimeSecs = 10;
    private String greeting;
    private String name;

    public Greeter(int mockActivityTimeSecs, String greeting, String name) {
      this.mockActivityTimeSecs = mockActivityTimeSecs;
      this.greeting = greeting;
      this.name = name;
    }

    @Override
    public String call() throws Exception {
      try {
        LOGGER.info(
            "GREETER ("
                + greeting
                + ") sleeping "
                + mockActivityTimeSecs
                + " seconds on thread "
                + Thread.currentThread().getName());
        // spoof some long-running work that never iterates to allow us to heartbeat
        Thread.sleep(TimeUnit.SECONDS.toMillis(mockActivityTimeSecs));
        LOGGER.info(
            "GREETER (" + greeting + ") awakened after " + mockActivityTimeSecs + " seconds");
        return greeting + " " + name + "! from thread: " + Thread.currentThread().getName();
      } catch (InterruptedException ee) {
        LOGGER.info(
            "GREETER ("
                + greeting
                + ") interrupted from timeout of "
                + mockActivityTimeSecs
                + " - ABORTED!");
        throw ee;
      }
    }
  }
  /**
   * Implementation of our workflow activity interface. It overwrites our defined composeGreeting
   * method.
   */
  static class GreetingActivitiesImpl implements GreetingActivities {

    private static final Logger LOGGER = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

    @Override
    public String composeGreeting(String greeting, String name) {
      // simulate a random time this activity should execute for
      Random random = new Random();
      int activityDurationSecs =
          random.nextInt(GreetingWorkflowImpl.ACTIVITY_MAX_SLEEP_SECONDS - 5) + 5;
      // Get the activity execution context
      LOGGER.info("composeGreeting started with activityDurationSecs {}", activityDurationSecs);

      var greeter = new Greeter(activityDurationSecs, greeting, name);
      try {
        return HeartbeatUtils.withBackgroundHeartbeatAndActivity(
            Activity::getExecutionContext, greeter, 4, (unused) -> false);
      } catch (ApplicationFailure e) {
        LOGGER.error(
            "Caught Exception but rethrowing to show activities as failed due to cancellation", e);
        throw e;
      }
    }
  }

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) {

    // Get a Workflow service stub.
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
     *
     * In the {@link ActivityOptions} definition the
     * "setMaxConcurrentActivityExecutionSize" option sets the max number of parallel activity executions allowed
     * The "setMaxConcurrentActivityTaskPollers" option sets the number of simultaneous poll requests on activity task queue
     */
    Worker worker =
        factory.newWorker(
            TASK_QUEUE,
            WorkerOptions.newBuilder()
                .setMaxConcurrentActivityExecutionSize(100)
                .setMaxConcurrentActivityTaskPollers(1)
                .build());

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
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID + UUID.randomUUID().toString())
                .setTaskQueue(TASK_QUEUE)
                .build());

    /*
     * Execute our workflow and wait for it to complete. The call to our getGreeting method is
     * synchronous.
     */
    String greeting = workflow.getGreeting("World");

    // Display workflow execution results
    System.out.println(greeting);
    //    try {
    //      Thread.sleep(5000);
    //    } catch (InterruptedException e) {
    //      System.out.println("failed to sleep after the things");
    //    }
    System.exit(0);
  }
}
