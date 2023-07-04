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
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

/**
 * Sample Temporal workflow that demonstrates the workflow compensation capability.
 *
 * <p>Compensation deals with undoing or reversing work which has already successfully completed.
 * (also called SAGA). Temporal includes very powerful support for compensation which is showedcased
 * in this example.
 *
 * @see io.temporal.samples.bookingsaga.TripBookingSaga for another SAGA example.
 */
public class HelloSaga {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloSagaTaskQueue";

  // Define the workflow unique id
  static final String WORKFLOW_ID = "HelloSagaTaskWorkflow";

  /**
   * Define the child workflow interface. It must contain one method annotated with @WorkflowMethod
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface ChildWorkflowOperation {

    /**
     * Define the child workflow method. This method is executed when the child workflow is started.
     * The child workflow completes when the workflow method finishes execution.
     */
    @WorkflowMethod
    void execute(int amount);
  }

  // Define the child workflow implementation. It implements the execute workflow method
  public static class ChildWorkflowOperationImpl implements ChildWorkflowOperation {

    /*
     * Define the ActivityOperation stub. Activity stubs are proxies for activity invocations that
     * are executed outside of the workflow thread on the activity worker, that can be on a
     * different host. Temporal is going to dispatch the activity results back to the workflow and
     * unblock the stub as soon as activity is completed on the activity worker.
     *
     * <p>In the {@link ActivityOptions} definition the "setStartToCloseTimeout" option sets the
     * maximum time of a single Activity execution attempt. For this example it is set to 10
     * seconds.
     */
    ActivityOperation activity =
        Workflow.newActivityStub(
            ActivityOperation.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public void execute(int amount) {
      activity.execute(amount);
    }
  }

  /**
   * Define the child workflow compensation interface. It must contain one method annotated
   * with @WorkflowMethod
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface ChildWorkflowCompensation {

    /**
     * Define the child workflow compensation method. This method is executed when the child
     * workflow is started. The child workflow completes when the workflow method finishes
     * execution.
     */
    @WorkflowMethod
    void compensate(int amount);
  }

  // Define the child workflow compensation implementation. It implements the compensate child
  // workflow method
  public static class ChildWorkflowCompensationImpl implements ChildWorkflowCompensation {

    /*
     * Define the ActivityOperation stub. Activity stubs are proxies for activity invocations that
     * are executed outside of the workflow thread on the activity worker, that can be on a
     * different host. Temporal is going to dispatch activity results back to the workflow and
     * unblock the stub as soon as activity is completed on the activity worker.
     *
     * <p>In the {@link ActivityOptions} definition the"setStartToCloseTimeout" option sets the
     * maximum time of a single Activity execution attempt. For this example it is set to 10
     * seconds.
     */
    ActivityOperation activity =
        Workflow.newActivityStub(
            ActivityOperation.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public void compensate(int amount) {
      activity.compensate(amount);
    }
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
  public interface ActivityOperation {
    @ActivityMethod
    void execute(int amount);

    @ActivityMethod
    void compensate(int amount);
  }

  /**
   * Implementation of the workflow activity interface. It overwrites the defined execute and
   * compensate activity methods.
   */
  public static class ActivityOperationImpl implements ActivityOperation {

    @Override
    public void execute(int amount) {
      System.out.println("ActivityOperationImpl.execute() is called with amount " + amount);
    }

    @Override
    public void compensate(int amount) {
      System.out.println("ActivityCompensationImpl.compensate() is called with amount " + amount);
    }
  }

  /**
   * Define the main workflow interface. It must contain one method annotated with @WorkflowMethod
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface SagaWorkflow {

    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    void execute();
  }

  // Define the main workflow implementation. It implements the execute workflow method
  public static class SagaWorkflowImpl implements SagaWorkflow {

    /*
     * Define the ActivityOperation stub. Activity stubs are proxies for activity invocations that
     * are executed outside of the workflow thread on the activity worker, that can be on a
     * different host. Temporal is going to dispatch activity results back to the workflow and
     * unblock the stub as soon as activity is completed on the activity worker.
     *
     * <p>In the {@link ActivityOptions} definition the "setStartToCloseTimeout" option sets the
     * maximum time of a single Activity execution attempt. For this example it is set to 2 seconds.
     */
    ActivityOperation activity =
        Workflow.newActivityStub(
            ActivityOperation.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    @Override
    public void execute() {

      // {@link io.temporal.workflow.Saga} implements the logic to perform compensation operations
      Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());

      try {

        /*
         * First we show how to compensate sync child workflow invocations. We first create a child
         * workflow stub and execute its "execute" method. Then we create a stub of the child
         * compensation workflow and register it with Saga. At this point this compensation workflow
         * is not invoked. It is invoked explicitly when we actually want to invoke compensation
         * (via saga.compensate()).
         */
        ChildWorkflowOperation op1 = Workflow.newChildWorkflowStub(ChildWorkflowOperation.class);
        op1.execute(10);
        ChildWorkflowCompensation c1 =
            Workflow.newChildWorkflowStub(ChildWorkflowCompensation.class);
        saga.addCompensation(c1::compensate, -10);

        /*
         * Now we show compensation of workflow activities which are invoked asynchronously. We
         * invoke the activity "execute" method async. Then we register its "compensate" method as
         * the compensation method for it.
         *
         * <p>Again note that the compensation of this activity again is only explicitly invoked
         * (via saga.compensate()).
         */
        Promise<Void> result = Async.procedure(activity::execute, 20);
        saga.addCompensation(activity::compensate, -20);
        // get the result of the activity (blocking)
        result.get();

        /*
         * You can also supply an arbitrary lambda expression as a saga
         * compensation function.
         * Note that this compensation function is not associated with a child workflow
         * method or an activity method. It is associated with the currently executing
         * workflow method.
         *
         * Also note that here in this example we use System.out in the main workflow logic.
         * In production make sure to use Workflow.getLogger to log messages from workflow code.
         */
        saga.addCompensation(
            () -> System.out.println("Other compensation logic in main workflow."));

        /*
         * Here we throw a runtime exception on purpose to showcase
         * how to trigger compensation in case of an exception.
         * Note that compensation can be also triggered
         * without a specific exception being thrown. You can built in
         * compensation to be part of your core workflow business requirements,
         * meaning it can be triggered as part of your business logic.
         */
        throw new RuntimeException("some error");

      } catch (Exception e) {
        // we catch our exception and trigger workflow compensation
        saga.compensate();
      }
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
     * Register our workflow implementations with the worker. Since workflows are stateful in nature,
     * we need to register our workflow types.
     */
    worker.registerWorkflowImplementationTypes(
        HelloSaga.SagaWorkflowImpl.class,
        HelloSaga.ChildWorkflowOperationImpl.class,
        HelloSaga.ChildWorkflowCompensationImpl.class);

    /*
     * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(new ActivityOperationImpl());

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    // Create our workflow options
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setWorkflowId(WORKFLOW_ID).setTaskQueue(TASK_QUEUE).build();

    // Create the workflow client stub. It is used to start our workflow execution.
    HelloSaga.SagaWorkflow workflow =
        client.newWorkflowStub(HelloSaga.SagaWorkflow.class, workflowOptions);

    // Execute our workflow
    workflow.execute();
    System.exit(0);
  }
}
