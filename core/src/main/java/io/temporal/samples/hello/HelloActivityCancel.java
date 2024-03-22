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
import io.temporal.common.RetryOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloActivityCancel {

  static final String TASK_QUEUE = "HelloActivityTaskQueue";

  static final String WORKFLOW_ID = "HelloActivityWorkflow";

  @WorkflowInterface
  public interface GreetingWorkflow {

    @WorkflowMethod
    String getGreeting(String name);
  }

  @WorkflowInterface
  public interface ChildGreetingWorkflow {

    @WorkflowMethod
    String getGreeting(String name);
  }

  public static class ChildGreetingWorkflowImpl implements ChildGreetingWorkflow {

    @Override
    public String getGreeting(final String name) {

      Workflow.sleep(Duration.ofSeconds(30));

      return null;
    }
  }

  @ActivityInterface
  public interface GreetingActivities {

    @ActivityMethod(name = "greet")
    String composeGreeting(String greeting, String name);
  }

  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                .setStartToCloseTimeout(Duration.ofSeconds(20))
                .setHeartbeatTimeout(Duration.ofSeconds(3))
                .setRetryOptions(
                    RetryOptions.newBuilder().setInitialInterval(Duration.ofSeconds(10)).build())
                .build());

    @Override
    public String getGreeting(String name) {
      String hello = null;

      try {

        final List<Promise<String>> promises = new ArrayList<>();

        ChildWorkflowStub child =
            Workflow.newUntypedChildWorkflowStub(
                ChildGreetingWorkflow.class.getSimpleName(),
                ChildWorkflowOptions.newBuilder()
                    //
                    // .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
                    //
                    // .setCancellationType(ChildWorkflowCancellationType.WAIT_CANCELLATION_REQUESTED)
                    .setWorkflowId("Child_of_" + WORKFLOW_ID)
                    .build());
        promises.add(child.executeAsync(String.class, "Hello", name));

        // Wait for the child workflow to start before returning the result
        // Promise<WorkflowExecution> childExecution = child.getExecution();
        // WorkflowExecution childWorkflowExecution = childExecution.get();

        promises.add(Async.function(activities::composeGreeting, "Hello", name));

        for (int i = promises.size() - 1; i >= 0; i--) {

          try {
            promises.get(i).get();
          } catch (Exception e) {
            System.out.println("In for promise >>>>>>> " + e);
          }
        }

      } catch (Exception e) {
        System.out.println("Something cancelled " + e);
        // throw e;
      }

      return hello;
    }
  }

  static class GreetingActivitiesImpl implements GreetingActivities {
    private static final Logger log = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

    @Override
    public String composeGreeting(String greeting, String name) {
      log.info("Composing greeting...");

      // int i = 0;

      try {

        for (int i = 0; i < 15; i++) {

          System.out.println(">>>>>>> heartbeat " + i);
          try {
            Activity.getExecutionContext().heartbeat("" + i);
          } catch (Exception e) {
            System.out.println(">>>>>>> heartbeat " + e);

            // return greeting + " " + name + "!";
            throw e;
          }

          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      } catch (Exception e) {
        System.out.println(">>>>>>> " + e);

        // return greeting + " " + name + "!";
        throw e;
      }
      return greeting + " " + name + "!";
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
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    worker.registerWorkflowImplementationTypes(ChildGreetingWorkflowImpl.class);

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
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    WorkflowClient.start(workflow::getGreeting, "World");

    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    System.out.println("About to cancel.. ");
    client.newUntypedWorkflowStub(WORKFLOW_ID).cancel();
    System.out.println("cancellation request sent .. ");

    //    System.exit(0);
  }
}
