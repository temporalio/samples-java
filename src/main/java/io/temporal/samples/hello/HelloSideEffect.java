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
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;

/**
 * Sample Temporal workflow that shows use of workflow SideEffect.
 *
 * <p>Workflow methods must be deterministic. In order to execute non-deterministic code, such as
 * random number generation as shown in this example, you should use Workflow.SideEffect
 *
 * <p>Note: you should not use SideEffect function to modify the workflow state. For that you should
 * only use the SideEffects return value!
 *
 * <p>To execute this example a locally running Temporal service instance is required. You can
 * follow instructions on how to set up your Temporal service here:
 * https://github.com/temporalio/temporal/blob/master/README.md#download-and-start-temporal-server-locally
 */
public class HelloSideEffect {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloSideEffectTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloSideEffectTaskWorkflow";

  /**
   * Define the Workflow Interface. It must contain one method annotated with @WorkflowMethod.
   *
   * <p>Workflow code includes core processing logic. It that shouldn't contain any heavyweight
   * computations, non-deterministic code, network calls, database operations, etc. All those things
   * should be handled by Activities.
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface SideEffectWorkflow {

    /**
     * This method is executed when the workflow is started. The workflow completes when the
     * workflow method finishes execution.
     */
    @WorkflowMethod
    String start();
  }

  /**
   * Define the Activity Interface. Activities are building blocks of any temporal workflow and
   * contain any business logic that could perform long running computation, network calls, etc.
   *
   * <p>Annotating activity methods with @ActivityMethod is optional
   *
   * @see io.temporal.activity.ActivityInterface
   * @see io.temporal.activity.ActivityMethod
   */
  @ActivityInterface
  public interface SideEffectActivities {

    // Define your activity method which can be called during workflow execution
    String doSomething(String input);
  }

  // Define the workflow implementation which implements our execute workflow method.
  public static class SideEffectWorkflowImpl implements SideEffectWorkflow {

    /**
     * Define the SideEffectActivities stub. Activity stubs are proxies for activity invocations
     * that are executed outside of the workflow thread on the activity worker, that can be on a
     * different host. Temporal is going to dispatch the activity results back to the workflow and
     * unblock the stub as soon as activity is completed on the activity worker.
     *
     * <p>Let's take a look at each {@link ActivityOptions} defined: The "setScheduleToCloseTimeout"
     * option sets the overall timeout that our workflow is willing to wait for activity to
     * complete. For this example it is set to 2 seconds.
     */
    private final SideEffectActivities activities =
        Workflow.newActivityStub(
            SideEffectActivities.class,
            ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(2)).build());

    int randomInt, savedRandomInt;
    String randomID, savedRandomID;
    int sideEffectsRandomInt, savedSideEffectsRandomInt;
    String sideEffectsRandomID, savedSideEffectsRandomID;
    int unsafeRandomInt, savedUnsafeRandomInt;

    @Override
    public String start() {

      // Replay-safe way to create random number using Workflow.newRandom
      randomInt = Workflow.newRandom().nextInt();
      savedRandomInt = Workflow.sideEffect(int.class, () -> randomInt);

      // Replay-safe way to create random uuid
      randomID = Workflow.randomUUID().toString();
      savedRandomID = Workflow.sideEffect(String.class, () -> randomID);

      /*
       * Random number using side effects. Note that this value is recorded in workflow history.
       * On replay the same value is returned so determinism is guaranteed.
       */
      sideEffectsRandomInt = Workflow.sideEffect(int.class, () -> new Random().nextInt());
      savedSideEffectsRandomInt = Workflow.sideEffect(int.class, () -> sideEffectsRandomInt);

      /*
       * Random ID using side effects. Note that this value is recorded in workflow history.
       * On replay the same value is returned so determinism is guaranteed.
       */
      sideEffectsRandomID = Workflow.sideEffect(String.class, UUID.randomUUID()::toString);
      savedSideEffectsRandomID = Workflow.sideEffect(String.class, () -> sideEffectsRandomID);

      /**
       * Let's now show unsafe (not-deterministic way) If we use the new Random().nextInt() it will
       * be re-evaluated on replay However savedUnsafeRandomInt use sideEffect and will have the
       * save return value on replay.
       */
      unsafeRandomInt = new Random().nextInt();
      savedUnsafeRandomInt = Workflow.sideEffect(int.class, () -> unsafeRandomInt);

      /**
       * Execute activity (sync) - with sticky exec disabled (see WorkerFactory creation in main)
       */
      String result = activities.doSomething("Hello");

      if (randomInt != savedRandomInt) {
        throw new IllegalStateException("Random ints not equal");
      }

      if (sideEffectsRandomInt != savedSideEffectsRandomInt) {
        throw new IllegalStateException("Sideffect random ints not equal");
      }

      if (!randomID.equals(savedRandomID)) {
        throw new IllegalStateException("Random IDs not equal");
      }

      if (!sideEffectsRandomID.equals(savedSideEffectsRandomID)) {
        throw new IllegalStateException("Sideffect random IDs not equal");
      }

      // This check will fail as unsafeRandomInt it was created not safely
      // and breaks workflow determinism
      if (unsafeRandomInt != savedUnsafeRandomInt) {
        return "Unsafely created random numbers are not equal";
      }

      return result;
    }
  }

  /** Simple activity implementation. */
  static class SideEffectActivitiesImpl implements SideEffectActivities {
    @Override
    public String doSomething(String input) {
      return "Received input: " + input;
    }
  }

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) {

    // Define the workflow service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();

    /*
     * Define the workflow client. It is a Temporal service client used to start, signal, and query
     * workflows
     */
    WorkflowClient client = WorkflowClient.newInstance(service);

    /*
     * Define the workflow factory. It is used to create workflow workers for a specific task queue.
     * For this example we set the setWorkflowHostLocalTaskQueueScheduleToStartTimeout to zero
     * (to disable sticky execution)
     */
    WorkerFactory factory =
        WorkerFactory.newInstance(
            client,
            WorkerFactoryOptions.newBuilder()
                .setWorkflowHostLocalTaskQueueScheduleToStartTimeout(Duration.ZERO)
                .build());

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
    worker.registerWorkflowImplementationTypes(SideEffectWorkflowImpl.class);

    /*
     Register our workflow activity implementation with the worker. Since workflow activities are
     stateless and thread-safe, we need to register a shared instance.
    */
    worker.registerActivitiesImplementations(new SideEffectActivitiesImpl());

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    // Create the workflow client stub. It is used to start our workflow execution.
    SideEffectWorkflow workflow =
        client.newWorkflowStub(
            SideEffectWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    /*
     * Execute our workflow and wait for it to complete. The call to our start method is
     * synchronous.
     *
     * See {@link io.temporal.samples.hello.HelloSignal} for an example of starting workflow
     * without waiting synchronously for its result.
     */
    String result = workflow.start();

    // Note that result should be
    // "Unsafely created random numbers are not equal"
    System.out.println(result);
    System.exit(0);
  }
}
