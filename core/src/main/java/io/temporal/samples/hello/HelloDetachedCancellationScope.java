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
import io.temporal.activity.ActivityCancellationType;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.ActivityFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This sample Temporal Workflow Definition demonstrates how to run "cleanup" code when a Workflow
 * Execution has been explicitly cancelled.
 */
public class HelloDetachedCancellationScope {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloDetachedCancellationTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloDetachedCancellationWorkflow";

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
    String getGreeting(String name);

    /** Query method to get the greeting */
    @QueryMethod
    String queryGreeting();
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
    String sayHello(String name);

    String sayGoodBye(String name);
  }

  /** This is the Greeting Activity Definition. */
  static class GreetingActivitiesImpl implements GreetingActivities {
    @Override
    public String sayHello(String name) {
      // This simulates a long-running Activity Execution so we can cancel the Workflow Execution
      // before it completes.
      for (int i = 0; i < Integer.MAX_VALUE; i++) {
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          // Wrap and re-throw the exception.
          throw Activity.wrap(e);
        }
        Activity.getExecutionContext().heartbeat(i);
      }
      return "unreachable";
    }

    @Override
    public String sayGoodBye(String name) {
      return "Goodbye " + name + "!";
    }
  }

  /** This is the Workflow Definition which implements our getGreeting method. */
  public static class GreetingWorkflowImpl implements GreetingWorkflow {
    private String greeting;

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                .setHeartbeatTimeout(Duration.ofSeconds(2))
                .build());

    @Override
    public String getGreeting(String name) {
      try {
        this.greeting = activities.sayHello(name);
        return greeting;
      } catch (ActivityFailure af) {
        // Create a CancellationScope that is not linked to a parent scope
        // This can be used in the "cleanup" code after the Workflow Execution has been cancelled.
        CancellationScope detached =
            Workflow.newDetachedCancellationScope(() -> greeting = activities.sayGoodBye(name));
        detached.run();
        throw af;
      }
    }

    @Override
    public String queryGreeting() {
      return greeting;
    }
  }

  public static void main(String[] args) throws InterruptedException {

    // Get a Workflow service stub.
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
     * Register our Workflow Types with the Worker. Workflow Types must be known to the Worker at
     * runtime.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    /*
     * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    /*
     * Start all the Workers that are in this process. The Workers will then start polling for
     * Workflow Tasks and Activity Tasks.
     */
    factory.start();

    // Create the Workflow client stub in order to start our Workflow Execution.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    WorkflowClient.start(workflow::getGreeting, "John");

    WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);

    Thread.sleep(1000);

    // Cancel the Workflow Execution
    // Note that this can be done from a different client.
    workflowStub.cancel();

    String result;

    try {
      // Wait for Workflow Execution results
      // Because we cancelled the Workflow Execution we should get WorkflowFailedException
      result = workflowStub.getResult(6, TimeUnit.SECONDS, String.class, String.class);
    } catch (TimeoutException | WorkflowFailedException e) {
      // Query the Workflow Execution to get the result which was set by the detached cancellation
      // scope run
      result = workflowStub.query("queryGreeting", String.class);
    }

    System.out.println(result);

    System.exit(0);
  }
}
