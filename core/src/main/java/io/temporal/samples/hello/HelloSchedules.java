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
import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.Payload;
import io.temporal.api.enums.v1.ScheduleOverlapPolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.schedules.*;
import io.temporal.common.converter.GlobalDataConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

/**
 * Sample Temporal workflow that demonstrates periodic workflow execution using a schedule. Schedule
 * is a new feature in Temporal designed to replace Cron workflows. Schedules allow for greater
 * control over when workflows are run and how they are run.
 */
public class HelloSchedules {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloScheduleTaskQueue";

  // Define the workflow unique id
  static final String WORKFLOW_ID = "HelloScheduleWorkflow";

  // Define the schedule unique id
  static final String SCHEDULE_ID = "HelloSchedule";

  /**
   * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
   *
   * <p>Workflow Definitions should not contain any heavyweight computations, non-deterministic
   * code, network calls, database operations, etc. Those things should be handled by the
   * Activities.
   *
   * @see WorkflowInterface
   * @see WorkflowMethod
   */
  @WorkflowInterface
  public interface GreetingWorkflow {

    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    void greet(String name);
  }

  /**
   * This is the Activity Definition's Interface. Activities are building blocks of any Temporal
   * Workflow and contain any business logic that could perform long running computation, network
   * calls, etc.
   *
   * <p>Annotating Activity Definition methods with @ActivityMethod is optional.
   *
   * @see ActivityInterface
   * @see io.temporal.activity.ActivityMethod
   */
  @ActivityInterface
  public interface GreetingActivities {

    // Define your activity method which can be called during workflow execution
    void greet(String greeting);
  }

  // Define the workflow implementation which implements the greet workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    /**
     * Define the GreetingActivities stub. Activity stubs are proxies for activity invocations that
     * are executed outside of the workflow thread on the activity worker, that can be on a
     * different host. Temporal is going to dispatch the activity results back to the workflow and
     * unblock the stub as soon as activity is completed on the activity worker.
     *
     * <p>In the {@link ActivityOptions} definition the "setStartToCloseTimeout" option sets the
     * maximum time of a single Activity execution attempt. For this example it is set to 10
     * seconds.
     */
    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public void greet(String name) {
      // Workflow Executions started by a Schedule have the following
      // additional properties appended to their search attributes.
      Payload scheduledByIDPayload =
          Workflow.getInfo().getSearchAttributes().getIndexedFieldsOrThrow("TemporalScheduledById");
      String scheduledByID =
          GlobalDataConverter.get().fromPayload(scheduledByIDPayload, String.class, String.class);

      Payload startTimePayload =
          Workflow.getInfo()
              .getSearchAttributes()
              .getIndexedFieldsOrThrow("TemporalScheduledStartTime");
      Instant startTime =
          GlobalDataConverter.get().fromPayload(startTimePayload, Instant.class, Instant.class);

      activities.greet(
          "Hello " + name + " from " + scheduledByID + " scheduled at " + startTime + "!");
    }
  }

  /**
   * Implementation of the workflow activity interface. It overwrites the defined greet activity
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
   * With the Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) throws InterruptedException {

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
     * Register the workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    /*
     * Register the workflow activity implementation with the worker. Since workflow activities are
     * stateless and thread-safe, we need to register a shared instance.
     */
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    /*
     * Get a Schedule client which can be used to interact with schedule.
     */
    ScheduleClient scheduleClient = ScheduleClient.newInstance(service);

    /*
     * Create the workflow options for our schedule.
     * Note: Not all workflow options are supported for schedules.
     */
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setWorkflowId(WORKFLOW_ID).setTaskQueue(TASK_QUEUE).build();

    /*
     * Create the action that will be run when the schedule is triggered.
     */
    ScheduleActionStartWorkflow action =
        ScheduleActionStartWorkflow.newBuilder()
            .setWorkflowType(HelloSchedules.GreetingWorkflow.class)
            .setArguments("World")
            .setOptions(workflowOptions)
            .build();

    // Define the schedule we want to create
    Schedule schedule =
        Schedule.newBuilder().setAction(action).setSpec(ScheduleSpec.newBuilder().build()).build();

    // Create a schedule on the server
    ScheduleHandle handle =
        scheduleClient.createSchedule(SCHEDULE_ID, schedule, ScheduleOptions.newBuilder().build());

    // Manually trigger the schedule once
    handle.trigger(ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_ALLOW_ALL);

    // Update the schedule with a spec, so it will run periodically
    handle.update(
        (ScheduleUpdateInput input) -> {
          Schedule.Builder builder = Schedule.newBuilder(input.getDescription().getSchedule());

          builder.setSpec(
              ScheduleSpec.newBuilder()
                  // Run the schedule at 5pm on Friday
                  .setCalendars(
                      Collections.singletonList(
                          ScheduleCalendarSpec.newBuilder()
                              .setHour(Collections.singletonList(new ScheduleRange(17)))
                              .setDayOfWeek(Collections.singletonList(new ScheduleRange(5)))
                              .build()))
                  // Run the schedule every 5s
                  .setIntervals(
                      Collections.singletonList(new ScheduleIntervalSpec(Duration.ofSeconds(5))))
                  .build());
          // Make the schedule paused to demonstrate how to unpause a schedule
          builder.setState(
              ScheduleState.newBuilder()
                  .setPaused(true)
                  .setLimitedAction(true)
                  .setRemainingActions(10)
                  .build());
          return new ScheduleUpdate(builder.build());
        });

    // Unpause schedule
    handle.unpause();

    // Wait for the schedule to run 10 actions
    while (true) {
      ScheduleState state = handle.describe().getSchedule().getState();
      if (state.getRemainingActions() == 0) {
        break;
      }
      Thread.sleep(5000);
    }
    // Delete the schedule once the sample is done
    handle.delete();
    System.exit(0);
  }
}
