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
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

/**
 * Sample Temporal workflow that demonstrates the use of multiple activities which extend a common
 * interface.
 *
 * <p>To execute this example a locally running Temporal service instance is required. You can
 * follow instructions on how to set up your Temporal service here:
 * https://github.com/temporalio/temporal/blob/master/README.md#download-and-start-temporal-server-locally
 */
public class HelloPolymorphicActivity {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloPolymorphicActivityTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloPolymorphicActivityWorkflow";

  /**
   * Define the Workflow Interface. It must contain at least one method annotated
   * with @WorkflowMethod
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @WorkflowInterface
  public interface GreetingWorkflow {

    /**
     * Define the workflow method. This method is executed when the workflow is started. The
     * workflow completes when the workflow method finishes execution.
     */
    @WorkflowMethod
    String getGreeting(String name);
  }

  // Define the base interface for our two workflow activities
  // Note it is not annotated with @ActivityInterface
  public interface GreetingActivity {
    String composeGreeting(String name);
  }

  /**
   * Define our first activity interface. Workflow methods can call activities during execution.
   * Annotating activity methods with @ActivityMethod is optional
   *
   * <p>Note our activity interface extends the base GreetingActivity interface. Also note that in
   * order to void the collisions in the activity name (which is by default the name of the activity
   * method) we set the namePrefix annotation parameter.
   *
   * @see io.temporal.activity.ActivityInterface
   * @see io.temporal.activity.ActivityMethod
   */
  @ActivityInterface(namePrefix = "Hello_")
  public interface HelloActivity extends GreetingActivity {
    @Override
    String composeGreeting(String name);
  }

  /**
   * Define our second activity interface. Workflow methods can call activities during execution.
   * Annotating activity methods with @ActivityMethod is optional
   *
   * <p>Note our activity interface extends the base GreetingActivity interface. Also note that in
   * order to void the collisions in the activity name (which is by default the name of the activity
   * method) we set the namePrefix annotation parameter.
   *
   * @see io.temporal.activity.ActivityInterface
   * @see io.temporal.activity.ActivityMethod
   */
  @ActivityInterface(namePrefix = "Bye_")
  public interface ByeActivity extends GreetingActivity {
    @Override
    String composeGreeting(String name);
  }

  // Define the workflow implementation. It implements our getGreeting workflow method
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    /**
     * Define the GreetingActivities stubs. Activity stubs implements activity interfaces and proxy
     * calls to it to Temporal activity invocations. Since Temporal activities are reentrant, a
     * single activity stub can be used for multiple activity invocations.
     *
     * <p>For this example we define two activity stubs, one for each of our defined activities.
     *
     * <p>Let's take a look at each {@link ActivityOptions} defined: The "setScheduleToCloseTimeout"
     * option sets the overall timeout that our workflow is willing to wait for activity to
     * complete. For this example it is set to 2 seconds for each of our activities.
     */
    private final GreetingActivity[] activities =
        new GreetingActivity[] {
          Workflow.newActivityStub(
              HelloActivity.class,
              ActivityOptions.newBuilder()
                  .setScheduleToCloseTimeout(Duration.ofSeconds(2))
                  .build()),
          Workflow.newActivityStub(
              ByeActivity.class,
              ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(2)).build())
        };

    @Override
    public String getGreeting(String name) {
      StringBuilder result = new StringBuilder();

      /*
       * Call the composeGreeting activity method
       * for each of our two activities.
       * Notice how you can use the common activities interface for each.
       *
       * Append the result of each of the activity invocation results and return it.
       */
      for (GreetingActivity activity : activities) {
        result.append(activity.composeGreeting(name));
        result.append('\n');
      }
      return result.toString();
    }
  }

  // Hello workflow activity implementation
  static class HelloActivityImpl implements HelloActivity {
    @Override
    public String composeGreeting(String name) {
      return "Hello " + name + "!";
    }
  }

  // Bye workflow activity implementation
  static class ByeActivityImpl implements ByeActivity {
    @Override
    public String composeGreeting(String name) {
      return "Bye " + name + "!";
    }
  }

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method is our
   * workflow starter.
   */
  public static void main(String[] args) {
    /*
     * Define the workflow service. It is a gRPC stubs wrapper which talks to the docker instance of
     * our locally running Temporal service.
     */
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();

    /*
     * Define the workflow client. It is a Temporal service client used to start, signal, and query
     * workflows
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
     * Register our workflow implementation with the worker. Since workflows are stateful in nature,
     * we need to register our workflow type.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    /*
     Register our workflow activities implementations with the worker. Since workflow activities are
     stateless and thread-safe, we need to register a shared instance.
    */
    worker.registerActivitiesImplementations(new HelloActivityImpl(), new ByeActivityImpl());

    // Start all the workers registered for a specific task queue.
    factory.start();

    // Create our workflow client stub. It is used to start our workflow execution.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    /*
     * Execute our workflow and wait for it to complete. The call to our getGreeting method is
     * synchronous.
     *
     * See {@link io.temporal.samples.hello.HelloSignal} for an example of starting workflow
     * without waiting synchronously for its result.
     */
    String greeting = workflow.getGreeting("World");

    // Print the workflow results. It should contain the results
    // of both of our defined activities
    System.out.println(greeting);
    System.exit(0);
  }
}
