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
 * This sample Temporal Workflow Definition demonstrates how to run "cleanup" code when a Workflow Execution has been
 * explicitly cancelled.
 *
 * <p>To execute this example a locally running Temporal service instance is required. You can
 * follow instructions on how to set up your Temporal service here:
 * https://github.com/temporalio/temporal/blob/master/README.md#download-and-start-temporal-server-locally
 */
public class HelloDetachedCancellationScope {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloDetachedCancellationTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloDetachedCancellationWorkflow";

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
  public interface GreetingWorkflow {

    /**
     * This method is executed when the workflow is started. The workflow completes when the
     * workflow method finishes execution.
     */
    @WorkflowMethod
    String getGreeting(String name);

    /** Query method to get the greeting */
    @QueryMethod
    String queryGreeting();
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
  public interface GreetingActivities {
    String sayHello(String name);

    String sayGoodBye(String name);
  }

  /** Implementation of the Greeting Activity. */
  static class GreetingActivitiesImpl implements GreetingActivities {
    @Override
    public String sayHello(String name) {
      // Simulate some long-running activity so we can cancel workflow before it completes
      for (int i = 0; i < Integer.MAX_VALUE; i++) {
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          // Rethrow the exception as runtime one
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

  /** Define the workflow implementation which implements our getGreeting workflow method. */
  public static class GreetingWorkflowImpl implements GreetingWorkflow {
    private String greeting;

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
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

    // Define the workflow service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();

    /**
     * Define the workflow client. It is a Temporal service client used to start, signal, and query
     * workflows
     */
    WorkflowClient client = WorkflowClient.newInstance(service);

    /**
     * Define the workflow factory. It is used to create workflow workers for a specific task queue.
     */
    WorkerFactory factory = WorkerFactory.newInstance(client);

    /**
     * Define the workflow worker. Workflow workers listen to a defined task queue and process
     * workflows and activities.
     */
    Worker worker = factory.newWorker(TASK_QUEUE);

    /**
     * Register our workflow implementation with the worker. Workflow implementations must be known
     * to the worker at runtime in order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    /**
     * Register our workflow activity implementation with the worker. Since workflow activities are
     * stateless and thread-safe, we need to register a shared instance.
     */
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    /**
     * Start all the workers registered for a specific task queue. The started workers then start
     * polling for workflows and activities.
     */
    factory.start();

    // Create the workflow client stub. It is used to start our workflow execution.
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

    // Cancel Workflow execution
    // This can be done from a different client for example
    workflowStub.cancel();

    String result;

    try {
      // Wait for workflow results
      // Because we cancelled the workflow we should get WorkflowFailedException
      result = workflowStub.getResult(6, TimeUnit.SECONDS, String.class, String.class);
    } catch (TimeoutException | WorkflowFailedException e) {
      // Query the workflow to get the result which was set by the detached cancellation scope run
      result = workflowStub.query("queryGreeting", String.class);
    }

    System.out.println(result);

    System.exit(0);
  }
}
