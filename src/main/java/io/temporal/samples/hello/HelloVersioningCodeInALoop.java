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
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import io.temporal.workflow.unsafe.WorkflowUnsafe;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/** Sample Temporal Workflow Definition that illustrates how to version code inside a loop. */
public class HelloVersioningCodeInALoop {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloVersioningInALoopTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloVersioningInALoopWorkflow";

  /**
   * With our Workflow defined, we can now start execution. The main method:
   *
   * <ul>
   *   <li>register a workflow implementation and starts the worker
   *   <li>start the workflow
   *   <li>stop the worker
   *   <li>register a different workflow implementation, that contains the code versioned inside a
   *       loop
   * </ul>
   */
  public static void main(String[] args) {

    WorkerFactory factory = startWorker(GreetingWorkflowImpl.class);

    // Create the workflow client stub. It is used to start and query our workflow execution.
    GreetingWorkflow workflow =
        factory
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId(WORKFLOW_ID)
                    .setTaskQueue(TASK_QUEUE)
                    .build());

    // Execute our workflow
    WorkflowExecution workflowExecution = WorkflowClient.start(workflow::execute);

    printActivitiesExecutedTimes(workflow);

    // In real life, to version the workflow code we have to stop the worker, modify the workflow
    // code and start
    // the worker again. Here we stop the worker and start it again with a different implementation
    // of the same workflow type

    // Shutdown the worker to force it replaying the event history to continue with the workflow
    // execution
    factory.shutdown();

    // start with a different implementation, that has the code versioned inside the loop
    factory = startWorker(GreetingWorkflowV1Impl.class);

    printActivitiesExecutedTimes(workflow);

    workflow.executeNextIteration();

    printActivitiesExecutedTimes(workflow);

    workflow.executeNextIteration();

    printActivitiesExecutedTimes(workflow);

    // Wait for the workflow to complete
    factory
        .getWorkflowClient()
        .newUntypedWorkflowStub(workflowExecution, Optional.empty())
        .getResult(Void.class);

    System.exit(0);
  }

  @NotNull
  private static WorkerFactory startWorker(Class<?> workflowImplType) {
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
    worker.registerWorkflowImplementationTypes(workflowImplType);

    /**
     * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(new ActivitiesImp());

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    return factory;
  }

  private static void printActivitiesExecutedTimes(GreetingWorkflow workflow) {
    System.out.println("ActivitiesExecutedTimes: " + workflow.getInfoActivitiesExecuted());
  }

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
    void execute();


    /**
     * Signal the workflow that is waiting inside the loop to execute to continue.
     */
    @SignalMethod
    void executeNextIteration();

    @QueryMethod
    ActivitiesExecuted getInfoActivitiesExecuted();
  }

  @ActivityInterface
  public interface Activities {

    @ActivityMethod
    String performX();

    @ActivityMethod
    String performY();
  }

  // Define the workflow implementation which implements our start workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    final ActivitiesExecuted activitiesExecuted = new ActivitiesExecuted();

    final Activities activities =
        Workflow.newActivityStub(
            Activities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());
    boolean executeNextIteration;

    @Override
    public void execute() {
      for (int i = 0; i < 2; i++) {
        executeActivityPerformX();
        Workflow.await(() -> this.executeNextIteration);
        this.executeNextIteration = false;
      }
    }

    @Override
    public void executeNextIteration() {
      this.executeNextIteration = true;
    }

    protected void executeActivityPerformX() {
      activities.performX();
      activitiesExecuted.add("performX");
    }

    @Override
    public ActivitiesExecuted getInfoActivitiesExecuted() {
      return this.activitiesExecuted;
    }
  }

  // Define a workflow implementation that introduces the code versioned
  public static class GreetingWorkflowV1Impl extends GreetingWorkflowImpl {

    @Override
    public void execute() {

      for (int i = 0; i < 2; i++) {

        System.out.println(
            "Replaying event history = [" + WorkflowUnsafe.isReplaying() + "], iteration " + i);

        // To modify the code and don't break the determinism we have to version the code.
        // Workflow.getVersion() will return the same value given the same changeId. To version the
        // code inside a loop
        // we have to use a different changeId to allow the versioning to pick different version
        final String incrementalChangeId = "changeId-" + i;
        int version = Workflow.getVersion(incrementalChangeId, Workflow.DEFAULT_VERSION, 1);

        if (version == Workflow.DEFAULT_VERSION) {
          executeActivityPerformX();
        }

        if (version == 1) {
          executeActivityPerformY();
        }

        Workflow.await(() -> this.executeNextIteration);
        this.executeNextIteration = false;
      }
    }

    private void executeActivityPerformY() {
      activities.performY();
      activitiesExecuted.add("performY");
    }
  }

  public static class ActivitiesImp implements Activities {

    @Override
    public String performX() {
      return null;
    }

    @Override
    public String performY() {
      return null;
    }
  }

  public static class ActivitiesExecuted {

    final Map<String, Integer> activitiesExecuted = new HashMap<>();

    public void add(String activityName) {
      int numExecutions =
          activitiesExecuted.get(activityName) != null
              ? activitiesExecuted.get(activityName) + 1
              : 1;
      activitiesExecuted.put(activityName, numExecutions);
    }

    @Override
    public String toString() {
      return activitiesExecuted.toString();
    }
  }
}
