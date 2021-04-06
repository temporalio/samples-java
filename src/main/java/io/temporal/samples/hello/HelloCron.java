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
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

/**
 * Sample Temporal workflow that demonstrates periodic workflow execution using a cron. Note that
 * the periodic execution is based on a fixed delay (provided by the cron definition). To learn
 * about periodic execution with a dynamic delay checkout the {@link HelloPeriodic} example.
 *
 * <p>To execute this example a locally running Temporal service instance is required. You can
 * follow instructions on how to set up your Temporal service here:
 * https://github.com/temporalio/temporal/blob/master/README.md#download-and-start-temporal-server-locally
 */
public class HelloCron {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloCronTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloCronWorkflow";

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
    void greet(String name);
  }

  /**
   * Define the Activity Interface. Workflow methods can call activities during execution.
   * Annotating activity methods with @ActivityMethod is optional
   *
   * @see io.temporal.activity.ActivityInterface
   * @see io.temporal.activity.ActivityMethod
   */
  @ActivityInterface
  public interface GreetingActivities {

    // Define your activity method which can be called during workflow execution
    @ActivityMethod
    void greet(String greeting);
  }

  // Define the workflow implementation. It implements our greet workflow method
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    /**
     * Define the GreetingActivities stub. Activity stubs implements activity interfaces and proxy
     * calls to it to Temporal activity invocations. Since Temporal activities are reentrant, a
     * single activity stub can be used for multiple activity invocations.
     *
     * <p>Let's take a look at each {@link ActivityOptions} defined:
     *
     * <p>The "setScheduleToCloseTimeout" option sets the overall timeout that our workflow is
     * willing to wait for activity to complete. For this example it is set to 10 seconds.
     */
    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public void greet(String name) {
      activities.greet("Hello " + name + "!");
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
     * Register our workflow activity implementation with the worker. Since workflow activities are
     * stateless and thread-safe, we need to register a shared instance.
     */
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    // Start all the workers registered for a specific task queue.
    factory.start();

    /*
     * Define our workflow options. Note that the cron definition is not part of the
     * core workflow definition. Workflow options allow you to execute the same
     * workflow in different ways (for example with or without a cron, etc).
     *
     * Here we use setCronSchedule to define a cron for our workflow execution.
     * The cron format is parsed by the https://github.com/robfig/cron" library.
     * In addition to the standard "* * * * *" format Temporal also supports the "@every" as well as
     * other cron definition extensions. For example you could define "@every 2s" to define a cron definition
     * which executes our workflow every two seconds.
     *
     * The defined cron expression "* * * * *" means that our workflow should execute every minute.
     *
     * We also use setWorkflowExecutionTimeout to define the workflow execution total time (set to three minutes).
     * After this time, our workflow execution ends (and our cron will stop executing as well).
     *
     * The setWorkflowRunTimeout defines the amount of time after which a single workflow instance is terminated.
     *
     * So given all our settings in the WorkflowOptions we define the following:
     * "Execute our workflow once every minute for three minutes.
     * Once a workflow instance is started, terminate it after one minute (if its still running)"
     *
     */
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setWorkflowId(WORKFLOW_ID)
            .setTaskQueue(TASK_QUEUE)
            .setCronSchedule("* * * * *")
            .setWorkflowExecutionTimeout(Duration.ofMinutes(3))
            .setWorkflowRunTimeout(Duration.ofMinutes(1))
            .build();

    // Create our workflow client stub. It is used to start our workflow execution.
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    try {
      // start workflow execution
      WorkflowExecution execution = WorkflowClient.start(workflow::greet, "World");
      System.out.println("Started " + execution);
    } catch (WorkflowExecutionAlreadyStarted e) {
      // Thrown when a workflow with the same WORKFLOW_ID is currently running
      System.out.println("Already running as " + e.getExecution());
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
