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

import com.google.common.base.Throwables;
import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowException;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.io.IOException;
import java.time.Duration;

/**
 * Sample Temporal workflow that demonstrates exception propagation across workflow activities,
 * child workflow, parent workflow, and the workflow client.
 */
public class HelloException {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloExceptionTaskQueue";

  // Define the workflow unique id
  static final String WORKFLOW_ID = "HelloExceptionWorkflow";

  /**
   * Define the parent workflow interface. It must contain one method annotated with @WorkflowMethod
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface GreetingWorkflow {

    /**
     * Define the parent workflow method. This method is executed when the workflow is started. The
     * workflow completes when the workflow method finishes execution.
     */
    @WorkflowMethod
    String getGreeting(String name);
  }

  /**
   * Define the child workflow interface. It must contain one method annotated with @WorkflowMethod
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface GreetingChild {

    /**
     * Define the child workflow method. This method is executed when the workflow is started. The
     * workflow completes when the workflow method finishes execution.
     */
    @WorkflowMethod
    String composeGreeting(String greeting, String name);
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

    String composeGreeting(String greeting, String name);
  }

  // Define the parent workflow implementation. It implements the getGreeting workflow method
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    @Override
    public String getGreeting(String name) {

      // Create the child workflow stub
      GreetingChild child = Workflow.newChildWorkflowStub(GreetingChild.class);
      // Execute the child workflow
      return child.composeGreeting("Hello", name);
    }
  }

  // Define the child workflow implementation. It implements the composeGreeting workflow method
  public static class GreetingChildImpl implements GreetingChild {

    /*
     * Define the GreetingActivities stub. Activity stubs are proxies for activity invocations that
     * are executed outside of the workflow thread on the activity worker, that can be on a
     * different host. Temporal is going to dispatch the activity results back to the workflow and
     * unblock the stub as soon as activity is completed on the activity worker.
     *
     * <p>In the {@link ActivityOptions} definition the"setStartToCloseTimeout" option sets the
     * maximum time of a single Activity execution attempt. For this example it is set to 10
     * seconds.
     */
    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(1))
                .setScheduleToStartTimeout(Duration.ofSeconds(5))
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumAttempts(2)
                        .build())
                .build());

    @Override
    public String composeGreeting(String greeting, String name) {
      return activities.composeGreeting(greeting, name);
    }
  }

  /*
   * Implementation of the workflow activity interface. It overwrites the defined composeGreeting
   * activity method.
   */
  static class GreetingActivitiesImpl implements GreetingActivities {
    @Override
    public String composeGreeting(String greeting, String name) {
      try {
        // here we simulate IOException being thrown inside the activity method
        // in order to show how it propagates through the workflow execution
        throw new IOException(greeting + " " + name + "!");
      } catch (IOException e) {
        /*
         * Instead of adding the thrown exception to the activity method signature
         * wrap it using Workflow.wrap before re-throwing it.
         * The original checked exception will be unwrapped and attached as the cause to the
         * {@link io.temporal.failure.ActivityFailure}
         */
        throw Activity.wrap(e);
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
     * Register our workflow parent and child implementations with the worker.
     * Since workflows are stateful in nature, we need to register our workflow types.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class, GreetingChildImpl.class);

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

    // Create our workflow options
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setWorkflowId(WORKFLOW_ID).setTaskQueue(TASK_QUEUE).build();

    // Create the workflow client stub. It is used to start our workflow execution.
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    try {
      // Execute our parent workflow. This will call the child workflow, which then calls the
      // defined workflow activity. The workflow activity throws the exception.
      workflow.getGreeting("World");
      throw new IllegalStateException("unreachable");
    } catch (WorkflowException e) {

      /*
       * This stack trace should help you better understand
       * how exception propagation works with Temporal.
       *
       * Looking at the stack trace from bottom-up (to understand the propagation) we first have:
       * 1) Caused by: io.temporal.failure.ApplicationFailure: message='Hello World!', type='java.io.IOException'
       * this is the IOException thrown by our activity.
       * 2) Caused by: io.temporal.failure.ActivityFailure - indicates the execution failure of our activity
       * 3) Caused by: io.temporal.failure.ChildWorkflowFailure - indicates the failure of our child workflow execution
       * 4) io.temporal.client.WorkflowFailedException - indicates the failure of our workflow execution
       */
      System.out.println("\nStack Trace:\n" + Throwables.getStackTraceAsString(e));

      /* Now let's see if our original IOException was indeed propagated all the way to our
       * WorkflowException which we caught in our code.
       * To do this let's print out its root cause:
       */
      Throwable cause = Throwables.getRootCause(e);
      // here we should get our originally thrown IOException and the message "Hello World"
      System.out.println("\n Root cause: " + cause.getMessage());
    }
    System.exit(0);
  }
}
