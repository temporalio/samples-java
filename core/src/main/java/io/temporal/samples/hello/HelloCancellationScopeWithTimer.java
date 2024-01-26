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
import io.temporal.client.ActivityCompletionException;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class HelloCancellationScopeWithTimer {
  // Define the task queue name
  static final String TASK_QUEUE = "HelloCancellationScopeTimerTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloCancellationScopeTimerWorkflow";

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
  public interface CancellationWithTimerWorkflow {

    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    String execute(String input);
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
  public interface UpdateInfoActivities {
    String updateInfo(String input);
  }

  // Define the workflow implementation which implements our getGreeting workflow method.
  public static class CancellationWithTimerWorkflowImpl implements CancellationWithTimerWorkflow {
    private final UpdateInfoActivities activities =
        Workflow.newActivityStub(
            UpdateInfoActivities.class,
            ActivityOptions.newBuilder()
                // If heartbeat timeout is not set, activity heartbeats will be throttled to one
                // every 30 seconds, it also will not have a heartbeat timeout.
                .setHeartbeatTimeout(Duration.ofSeconds(2))
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                .build());

    private String result;

    @Override
    public String execute(String input) {
      // Create cancellation scope for our activity execution
      CancellationScope cancellationScope =
          Workflow.newCancellationScope(
              () -> {
                try {
                  result = activities.updateInfo(input);
                } catch (ActivityFailure cause) {
                  throw cause;
                }
              });

      // Create a timer, if this timer fires we want to cancel our activity and complete the
      // workflow execution
      // Giving client default result. Note for sample the tier is set to less than the
      // activity StartToClose timeout in order to simulate it getting cancelled
      Workflow.newTimer(Duration.ofSeconds(3))
          .thenApply(
              result -> {
                // Cancel our activity, note activity has to heartbeat to receive cancellation
                System.out.println("Cancelling scope as timer fired");
                cancellationScope.cancel();
                return null;
              });
      // Start our cancellation scope
      try {
        cancellationScope.run();
      } catch (ActivityFailure e) {
        // Activity cancellation is going thrigger activity failure
        // The cause of activity failure would be CanceledFailure
        if (e.getCause() instanceof CanceledFailure) {
          result = "Activity canceled due to timer firing.";
        } else {
          result = "Activity failed due to: " + e.getMessage();
        }
      }
      return result;
    }
  }

  /**
   * Implementation of our workflow activity interface. It overwrites our defined composeGreeting
   * method.
   */
  static class UpdateInfoActivitiesImpl implements UpdateInfoActivities {

    @Override
    public String updateInfo(String input) {
      // Get the activity execution context
      ActivityExecutionContext context = Activity.getExecutionContext();

      // Our "dummy" activity just sleeps for a second up to 10 times and heartbeats
      for (int i = 0; i < 10; i++) {
        sleep(1);
        try {
          context.heartbeat(i);
        } catch (ActivityCompletionException e) {
          // Here we can do some cleanup if needed before we re-throw activity completion exception
          throw e;
        }
      }

      return "dummy activity result";
    }

    private void sleep(int seconds) {
      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
      } catch (InterruptedException ee) {
        // Empty
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
     */
    Worker worker = factory.newWorker(TASK_QUEUE);

    /*
     * Register our workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(CancellationWithTimerWorkflowImpl.class);

    /*
     * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(new UpdateInfoActivitiesImpl());

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    // Create the workflow client stub. It is used to start our workflow execution.
    CancellationWithTimerWorkflow workflow =
        client.newWorkflowStub(
            CancellationWithTimerWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    /*
     * Execute our workflow and wait for it to complete. The call to our getGreeting method is
     * synchronous.
     */
    String result = workflow.execute("Some test input");

    // Display workflow execution results
    System.out.println(result);
    System.exit(0);
  }
}
