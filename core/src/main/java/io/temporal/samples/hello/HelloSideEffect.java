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
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;

/**
 * Sample Temporal workflow that shows use of workflow SideEffect.
 *
 * <p>Workflow methods must be deterministic. In order to execute non-deterministic code, such as
 * random number generation as shown in this example, you should use Workflow.SideEffect.
 * Workflow.SideEffect is typically used for very quick-running operations, where as Workflow
 * Activities or Local Activities, which can also execute non-deterministic code, are meant for more
 * expensive operations.
 *
 * <p>Note: you should not use SideEffect function to modify the workflow state. For that you should
 * only use the SideEffect's return value!
 */
public class HelloSideEffect {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloSideEffectTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloSideEffectTaskWorkflow";

  /**
   * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
   *
   * <p>Workflow code includes core processing logic. It shouldn't contain any heavyweight
   * computations, non-deterministic code, network calls, database operations, etc. All those things
   * should be handled by Activities.
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface SideEffectWorkflow {

    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    String execute();

    @QueryMethod
    String getResult();
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
  public interface SideEffectActivities {

    // Define your activity methods which can be called during workflow execution
    String sayHello(String greeting);

    String sayGoodBye(String greeting);
  }

  // Define the workflow implementation which implements our execute workflow method.
  public static class SideEffectWorkflowImpl implements SideEffectWorkflow {

    /**
     * Define the SideEffectActivities stub. Activity stubs are proxies for activity invocations
     * that are executed outside of the workflow thread on the activity worker, that can be on a
     * different host. Temporal is going to dispatch the activity results back to the workflow and
     * unblock the stub as soon as activity is completed on the activity worker.
     *
     * <p>In the {@link ActivityOptions} definition the "setStartToCloseTimeout" option sets maximum
     * time of a single Activity execution attempt. For this example it is set to 2 seconds.
     */
    private final SideEffectActivities activities =
        Workflow.newActivityStub(
            SideEffectActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    int randomInt, sideEffectsRandomInt;
    UUID randomUUID;
    String result;

    @Override
    public String execute() {

      // Replay-safe way to create random number using Workflow.newRandom
      randomInt = Workflow.newRandom().nextInt();

      // Replay-safe way to create random uuid
      randomUUID = Workflow.randomUUID();

      /*
       * Random number using side effects. Note that this value is recorded in workflow history. On
       * replay the same value is returned so determinism is guaranteed.
       */
      sideEffectsRandomInt =
          Workflow.sideEffect(
              int.class,
              () -> {
                Random random = new SecureRandom();
                return random.nextInt();
              });

      /*
       * Since our randoms are all created safely (using SideEffects or Workflow deterministic
       * methods) the workflow result should be same as the queries ran after workflow completion.
       * In the case we did not use safe methods, the queries could have a different result.
       */
      if ((randomUUID.version() + randomInt + sideEffectsRandomInt) % 2 == 0) {
        result = activities.sayHello("World");
      } else {
        result = activities.sayGoodBye("World!");
      }
      return result;
    }

    @Override
    public String getResult() {
      return result;
    }
  }

  /** Simple activity implementation. */
  static class SideEffectActivitiesImpl implements SideEffectActivities {
    @Override
    public String sayHello(String greeting) {
      return "Hello " + greeting;
    }

    @Override
    public String sayGoodBye(String greeting) {
      return "Goodbye " + greeting;
    }
  }

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) {

    // Define the workflow service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    /*
     * Get a Workflow service client which can be used to start, Signal, and Query Workflow
     * Executions.
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
     * Register our workflow implementation with the worker. Workflow implementations must be known
     * to the worker at runtime in order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(SideEffectWorkflowImpl.class);

    /*
     * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(new SideEffectActivitiesImpl());

    /*
     * Start all the workers registered for a specific task queue. The started workers then start
     * polling for workflows and activities.
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
     * <p>See {@link io.temporal.samples.hello.HelloSignal} for an example of starting workflow
     * without waiting synchronously for its result.
     */
    String result = workflow.execute();

    // Print workflow result
    System.out.println(result);

    // Note that query should return the exact same result
    System.out.println(workflow.getResult());

    System.exit(0);
  }
}
