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

import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

/**
 * Demonstrates async invocation of an entire sequence of activities. Requires a local instance of
 * Temporal server to be running.
 */
public class HelloAsyncLambda {

  static final String TASK_LIST = "HelloAsyncLambda";

  public interface GreetingWorkflow {
    /** @return greeting string */
    @WorkflowMethod
    String getGreeting(String name);
  }

  /** Activity interface is just a POJI. * */
  public interface GreetingActivities {
    String getGreeting();

    String composeGreeting(String greeting, String name);
  }

  /** GreetingWorkflow implementation that calls GreetingsActivities#printIt. */
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    /**
     * Activity stub implements activity interface and proxies calls to it to Temporal activity
     * invocations. Because activities are reentrant, only a single stub can be used for multiple
     * activity invocations.
     */
    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public String getGreeting(String name) {
      // Async.invoke accepts not only activity or child workflow method references
      // but lambda functions as well. Behind the scene it allocates a thread
      // to execute it asynchronously.
      Promise<String> result1 =
          Async.function(
              () -> {
                String greeting = activities.getGreeting();
                return activities.composeGreeting(greeting, name);
              });
      Promise<String> result2 =
          Async.function(
              () -> {
                String greeting = activities.getGreeting();
                return activities.composeGreeting(greeting, name);
              });
      return result1.get() + "\n" + result2.get();
    }
  }

  static class GreetingActivitiesImpl implements GreetingActivities {

    @Override
    public String getGreeting() {
      return "Hello";
    }

    @Override
    public String composeGreeting(String greeting, String name) {
      return greeting + " " + name + "!";
    }
  }

  public static void main(String[] args) {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task lists
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // Worker that listens on a task list and hosts both workflow and activity implementations.
    Worker worker = factory.newWorker(TASK_LIST);
    // Workflows are stateful. So you need a type to create instances.
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    // Activities are stateless and thread safe. So a shared instance is used.
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
    // Start listening to the workflow and activity task lists.
    factory.start();

    // Get a workflow stub using the same task list the worker uses.
    // As the required ExecutionStartToCloseTimeout is not specified through the @WorkflowMethod
    // annotation it has to be specified through the options.
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskList(TASK_LIST)
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    System.out.println(greeting);
    System.exit(0);
  }
}
