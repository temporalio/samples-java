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
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

/**
 * Demonstrates an asynchronous activity implementation. Requires a local instance of Temporal
 * server to be running.
 */
public class HelloAsyncActivityCompletion {

  static final String TASK_QUEUE = "HelloAsyncActivityCompletion";

  @WorkflowInterface
  public interface GreetingWorkflow {
    @WorkflowMethod
    String getGreeting(String name);
  }

  /** Activity interface is just a POJI. * */
  @ActivityInterface
  public interface GreetingActivities {
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
      // This is a blocking call that returns only after the activity has completed.
      return activities.composeGreeting("Hello", name);
    }
  }

  static class GreetingActivitiesImpl implements GreetingActivities {
    private final ActivityCompletionClient completionClient;

    GreetingActivitiesImpl(ActivityCompletionClient completionClient) {
      this.completionClient = completionClient;
    }

    /**
     * Demonstrates how to implement an activity asynchronously. When {@link
     * io.temporal.activity.ActivityExecutionContext#doNotCompleteOnReturn()} is called the activity
     * implementation function returning doesn't complete the activity.
     */
    @Override
    public String composeGreeting(String greeting, String name) {
      // TaskToken is a correlation token used to match an activity task with its completion
      ActivityExecutionContext context = Activity.getExecutionContext();
      byte[] taskToken = context.getTaskToken();
      // In real life this request can be executed anywhere. By a separate service for
      // example.
      ForkJoinPool.commonPool().execute(() -> composeGreetingAsync(taskToken, greeting, name));
      context.doNotCompleteOnReturn();
      // When doNotCompleteOnReturn() is invoked the return value is ignored.
      return "ignored";
    }

    private void composeGreetingAsync(byte[] taskToken, String greeting, String name) {
      String result = greeting + " " + name + "!";
      // To complete an activity from a different thread or process use ActivityCompletionClient.
      // In real applications the client is initialized by a process that performs the completion.
      completionClient.complete(taskToken, result);
    }
  }

  public static void main(String[] args) throws ExecutionException, InterruptedException {
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
    // CompletionClient is passed to activity here only to support unit testing.
    ActivityCompletionClient completionClient = client.newActivityCompletionClient();
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl(completionClient));
    // Start listening to the workflow and activity task queues.
    factory.start();

    // Start a workflow execution. Usually this is done from another program.
    // Uses task queue from the GreetingWorkflow @WorkflowMethod annotation.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    // Execute a workflow asynchronously returning a future that can be used to wait for the
    // workflow
    // completion.
    CompletableFuture<String> greeting = WorkflowClient.execute(workflow::getGreeting, "World");
    // Wait for workflow completion.
    System.out.println(greeting.get());
    System.exit(0);
  }
}
