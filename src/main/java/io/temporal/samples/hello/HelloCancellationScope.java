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
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.ActivityCancelledException;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Async;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.Random;

/**
 * The sample executes multiple activities in parallel. Then it waits for one of them to finish,
 * cancels all others and waits for their cancellation completion.
 *
 * <p>The cancellation is done through {@link CancellationScope}.
 *
 * <p>Note that ActivityOptions.cancellationType is set to WAIT_CANCELLATION_COMPLETED. Otherwise
 * the activity completion promise is not going to wait for the activity to finish cancellation.
 *
 * <p>TODO(maxim): Unit test
 */
public class HelloCancellationScope {

  static final String TASK_QUEUE = "HelloCancellationScope";

  @WorkflowInterface
  public interface GreetingWorkflow {
    @WorkflowMethod
    String getGreeting(String name);
  }

  @ActivityInterface
  public interface GreetingActivities {
    String composeGreeting(String greeting, String name);
  }

  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private static String[] greetings =
        new String[] {"Hello", "Bye", "Hola", "Привет", "Oi", "Hallo"};

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                .setScheduleToCloseTimeout(Duration.ofSeconds(100))
                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                .build());

    @Override
    public String getGreeting(String name) {
      Promise<String>[] results = new Promise[greetings.length];
      CancellationScope scope =
          Workflow.newCancellationScope(
              () -> {
                for (int i = 0; i < greetings.length; i++) {
                  results[i] = Async.function(activities::composeGreeting, greetings[i], name);
                }
              });
      // As code inside the scope is non blocking the run doesn't block.
      scope.run();
      // Wait for one of the activities to complete.
      Promise.anyOf(results).get();
      // Cancel all other activities
      scope.cancel();
      // Get the result from one of the Promises.
      String result = null;
      for (int i = 0; i < greetings.length; i++) {
        if (results[i].isCompleted()) {
          result = results[i].get();
          break;
        }
      }
      // Wait for all activities to complete.
      Promise.allOf(results).get();
      return result;
    }
  }

  static class GreetingActivitiesImpl implements GreetingActivities {

    @Override
    public String composeGreeting(String greeting, String name) {
      ActivityExecutionContext context = Activity.getExecutionContext();
      Random random = new Random();
      int count = random.nextInt(30);
      System.out.println("Activity for " + greeting + " going to take " + count + " seconds");
      for (int i = 0; i < count; i++) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          // Empty
        }
        try {
          context.heartbeat(i);
        } catch (ActivityCancelledException e) {
          System.out.println("Activity for " + greeting + " was cancelled");
          throw e;
        }
      }
      System.out.println("Activity for " + greeting + " completed");
      return greeting + " " + name + "!";
    }
  }

  public static void main(String[] args) {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // Worker that listens on a task queue and hosts both workflow and activity implementations.
    Worker worker = factory.newWorker(TASK_QUEUE);
    // Workflows are stateful. So you need a type to create instances.
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    // Activities are stateless and thread safe. So a shared instance is used.
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
    // Start listening to the workflow and activity task queues.
    factory.start();

    // Start a workflow execution. Usually this is done from another program.\n'
    // Uses task queue from the GreetingWorkflow @WorkflowMethod annotation.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    System.out.println(greeting);
    System.exit(0);
  }
}
