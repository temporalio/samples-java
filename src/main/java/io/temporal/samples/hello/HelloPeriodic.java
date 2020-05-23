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
import io.temporal.client.DuplicateWorkflowException;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.proto.common.WorkflowExecution;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.Optional;
import java.util.Random;

/**
 * Demonstrates a workflow that executes an activity periodically with random delay. Note that the
 * looping approach is useful when the delay between invocations is dynamically calculated. Use
 * existing cron feature demonstrated by {@link HelloCron} sample for a fixed periodic execution.
 *
 * <p>Requires a local instance of Temporal server to be running.
 */
public class HelloPeriodic {

  static final String TASK_LIST = "HelloPeriodic";
  static final String PERIODIC_WORKFLOW_ID = "HelloPeriodic";

  @WorkflowInterface
  public interface GreetingWorkflow {
    /**
     * Use single fixed ID to ensure that there is at most one instance running. To run multiple
     * instances set different IDs through WorkflowOptions passed to the
     * WorkflowClient.newWorkflowStub call.
     */
    @WorkflowMethod
    void greetPeriodically(String name);
  }

  @ActivityInterface
  public interface GreetingActivities {
    void greet(String greeting);
  }

  /**
   * GreetingWorkflow implementation that calls {@link #greetPeriodically(String)} continuously with
   * a specified interval.
   */
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    /**
     * This value is so low just to make the example interesting to watch. In real life you would
     * use something like 100 or a value that matches a business cycle. For example if it runs once
     * an hour 24 would make sense.
     */
    private final int CONTINUE_AS_NEW_FREQUENCEY = 10;

    /** To ensure determinism use {@link Workflow#newRandom()} to create random generators. */
    private final Random random = Workflow.newRandom();

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(10)).build());

    /**
     * Stub used to terminate this workflow run and create the next one with the same ID atomically.
     */
    private final GreetingWorkflow continueAsNew =
        Workflow.newContinueAsNewStub(GreetingWorkflow.class);

    @Override
    public void greetPeriodically(String name) {
      // Loop the predefined number of times then continue this workflow as new.
      // This is needed to periodically truncate the history size.
      for (int i = 0; i < CONTINUE_AS_NEW_FREQUENCEY; i++) {
        int delayMillis = random.nextInt(10000);
        activities.greet("Hello " + name + "! Sleeping for " + delayMillis + " milliseconds.");
        Workflow.sleep(delayMillis);
      }
      // Current workflow run stops executing after this call.
      continueAsNew.greetPeriodically(name);
      // unreachable line
    }
  }

  static class GreetingActivitiesImpl implements GreetingActivities {
    @Override
    public void greet(String greeting) {
      System.out.println("From " + Activity.getWorkflowExecution() + ": " + greeting);
    }
  }

  public static void main(String[] args) throws InterruptedException {
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

    // To ensure that this daemon type workflow is always running try to start it periodically
    // ignoring the duplicated exception.
    // It is only to protect from application level failures.
    // Failures of a workflow worker don't lead to workflow failures.
    WorkflowExecution execution = null;
    while (true) {
      // Print reason of failure of the previous run, before restarting.
      if (execution != null) {
        WorkflowStub workflow = client.newUntypedWorkflowStub(execution, Optional.empty());
        try {
          workflow.getResult(Void.class);
        } catch (WorkflowException e) {
          System.out.println("Previous instance failed:\n" + Throwables.getStackTraceAsString(e));
        }
      }
      // New stub instance should be created for each new workflow start.
      GreetingWorkflow workflow =
          client.newWorkflowStub(
              GreetingWorkflow.class,
              // At most one instance.
              WorkflowOptions.newBuilder()
                  .setWorkflowId(PERIODIC_WORKFLOW_ID)
                  .setTaskList(TASK_LIST)
                  .build());
      try {
        execution = WorkflowClient.start(workflow::greetPeriodically, "World");
        System.out.println("Started " + execution);
      } catch (DuplicateWorkflowException e) {
        System.out.println("Still running as " + e.getExecution());
      } catch (Throwable e) {
        e.printStackTrace();
        System.exit(1);
      }
      // This value is so low just for the sample purpose. In production workflow
      // it is usually much higher.
      Thread.sleep(10000);
    }
  }
}
