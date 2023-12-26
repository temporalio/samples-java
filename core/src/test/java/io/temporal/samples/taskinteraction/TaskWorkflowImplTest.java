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

package io.temporal.samples.taskinteraction;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.common.interceptors.*;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkerFactoryOptions;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;

public class TaskWorkflowImplTest {

  final CompletableFuture<Void>[] completedActivities =
      new CompletableFuture[] {new CompletableFuture<>()};

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkerFactoryOptions(
              WorkerFactoryOptions.newBuilder()
                  .setWorkerInterceptors(new MyWorkerInterceptor(completedActivities))
                  .validateAndBuildWithDefaults())
          .setWorkflowTypes(TaskWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testRunWorkflow() throws InterruptedException, ExecutionException {

    final TaskActivity activities = mock(TaskActivity.class);
    when(activities.createTask(any())).thenReturn("done");
    testWorkflowRule.getWorker().registerActivitiesImplementations(activities);

    testWorkflowRule.getTestEnvironment().start();

    // Get stub to the dynamically registered interface
    TaskWorkflow workflow = testWorkflowRule.newWorkflowStub(TaskWorkflow.class);

    // start workflow execution
    WorkflowExecution execution = WorkflowClient.start(workflow::execute);

    final TaskClient taskClient =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(TaskClient.class, execution.getWorkflowId());

    // Wait for the activity to get executed two times. Two tasks created
    completedActivities[0].get();

    final List<Task> asyncTasksStarted = taskClient.getOpenTasks();
    assertEquals(2, asyncTasksStarted.size());
    // Change tasks state, not completing them yet
    changeTaskState(taskClient, asyncTasksStarted, Task.STATE.STARTED);

    // The two tasks are still open, lets complete them
    final List<Task> asyncTasksPending = taskClient.getOpenTasks();
    assertEquals(2, asyncTasksPending.size());
    changeTaskState(taskClient, asyncTasksPending, Task.STATE.COMPLETED);

    // Wait for the activity to get executed for the third time. One more task gets created
    completedActivities[0].get();

    final List<Task> syncTask = taskClient.getOpenTasks();
    assertEquals(1, syncTask.size());
    changeTaskState(taskClient, syncTask, Task.STATE.STARTED);
    changeTaskState(taskClient, syncTask, Task.STATE.COMPLETED);

    // Workflow completes
    final WorkflowStub untyped =
        testWorkflowRule.getWorkflowClient().newUntypedWorkflowStub(execution.getWorkflowId());
    untyped.getResult(String.class);
  }

  private static void changeTaskState(TaskClient client, List<Task> tasks, Task.STATE state) {
    tasks.forEach(
        t -> {
          client.updateTask(
              new TaskService.TaskRequest(state, "Changing state to: " + state, t.getToken()));
        });
  }

  private final AtomicInteger integer = new AtomicInteger(0);

  private class OnActivityCompleteInterceptor extends ActivityInboundCallsInterceptorBase {
    private CompletableFuture<Void>[] completableFuture;

    public OnActivityCompleteInterceptor(
        final ActivityInboundCallsInterceptor next,
        final CompletableFuture<Void>[] completableFuture) {
      super(next);
      this.completableFuture = completableFuture;
    }

    @Override
    public ActivityOutput execute(final ActivityInput input) {
      final ActivityOutput execute = super.execute(input);
      integer.getAndIncrement();
      if (Arrays.asList(2, 3).contains(integer.get())) {
        completableFuture[0].complete(null);
        completableFuture[0] = new CompletableFuture<>();
      }
      return execute;
    }
  }

  private class MyWorkerInterceptor extends WorkerInterceptorBase {

    private CompletableFuture<Void>[] completableFuture;

    public MyWorkerInterceptor(final CompletableFuture<Void>[] completableFuture) {
      this.completableFuture = completableFuture;
    }

    public ActivityInboundCallsInterceptor interceptActivity(ActivityInboundCallsInterceptor next) {
      return new OnActivityCompleteInterceptor(next, this.completableFuture);
    }
  }
}
