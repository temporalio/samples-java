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

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.interceptors.*;
import io.temporal.samples.taskinteraction.activity.ActivityTaskImpl;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.workflow.Workflow;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Rule;
import org.junit.Test;

public class WorkflowWithTasksImplTest {

  private MyWorkerInterceptor myWorkerInterceptor = new MyWorkerInterceptor();

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkerFactoryOptions(
              WorkerFactoryOptions.newBuilder()
                  .setWorkerInterceptors(myWorkerInterceptor)
                  .validateAndBuildWithDefaults())
          .setDoNotStart(true)
          .build();

  @Test
  public void testEnd2End() {

    final WorkflowClient workflowClient = testWorkflowRule.getTestEnvironment().getWorkflowClient();
    testWorkflowRule
        .getWorker()
        .registerWorkflowImplementationTypes(
            WorkflowWithTasksImpl.class, WorkflowTaskManagerImpl.class);

    testWorkflowRule
        .getWorker()
        .registerActivitiesImplementations(new ActivityTaskImpl(workflowClient));

    testWorkflowRule.getTestEnvironment().start();

    WorkflowWithTasks workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                WorkflowWithTasks.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId(WorkflowWithTasks.WORKFLOW_ID)
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .build());

    WorkflowExecution execution = WorkflowClient.start(workflow::execute);

    // Wait until the first two tasks from WorkflowWithTasks are created in WorkflowTaskManager
    myWorkerInterceptor.waitUntilTwoCreateTaskInvocations();

    WorkflowTaskManager workflowManager =
        workflowClient.newWorkflowStub(WorkflowTaskManager.class, WorkflowTaskManager.WORKFLOW_ID);

    final List<Task> pendingTask = getPendingTask(workflowManager);
    assertEquals(2, pendingTask.size());

    // Complete the two pending task created in parallel from `WorkflowWithTasks`.
    // Send update to the workflow that keeps tasks state, that will signal back
    // the `WorkflowWithTasks` execution
    workflowManager.completeTaskByToken(pendingTask.get(0).getToken());
    workflowManager.completeTaskByToken(pendingTask.get(1).getToken());

    // Wait until the last task in WorkflowWithTasks is created in WorkflowTaskManager
    myWorkerInterceptor.waitUntilThreeInvocationsOfCreateTask();

    // Complete the last task in `WorkflowWithTasks`
    assertEquals(1, getPendingTask(workflowManager).size());
    workflowManager.completeTaskByToken(getPendingTask(workflowManager).get(0).getToken());

    // Wait workflow to complete
    workflowClient.newUntypedWorkflowStub(execution.getWorkflowId()).getResult(Void.class);

    final DescribeWorkflowExecutionResponse describeWorkflowExecutionResponse =
        getDescribeWorkflowExecutionResponse(workflowClient, execution);
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecutionResponse.getWorkflowExecutionInfo().getStatus());
  }

  private DescribeWorkflowExecutionResponse getDescribeWorkflowExecutionResponse(
      final WorkflowClient workflowClient, final WorkflowExecution execution) {
    return workflowClient
        .getWorkflowServiceStubs()
        .blockingStub()
        .describeWorkflowExecution(
            DescribeWorkflowExecutionRequest.newBuilder()
                .setNamespace(testWorkflowRule.getTestEnvironment().getNamespace())
                .setExecution(execution)
                .build());
  }

  private static List<Task> getPendingTask(final WorkflowTaskManager workflowManager) {
    return workflowManager.getPendingTask();
  }

  private class MyWorkerInterceptor extends WorkerInterceptorBase {

    private int createTaskInvocations = 0;

    private CompletableFuture<Void> waitUntilTwoInvocationsOfCreateTask;

    private CompletableFuture<Void> waitUntilThreeInvocationsOfCreateTask;

    public MyWorkerInterceptor() {
      waitUntilTwoInvocationsOfCreateTask = new CompletableFuture<>();
      waitUntilThreeInvocationsOfCreateTask = new CompletableFuture<>();
    }

    public Void waitUntilTwoCreateTaskInvocations() {
      return getFromCompletableFuture(waitUntilTwoInvocationsOfCreateTask);
    }

    public Void waitUntilThreeInvocationsOfCreateTask() {
      return getFromCompletableFuture(waitUntilThreeInvocationsOfCreateTask);
    }

    private Void getFromCompletableFuture(final CompletableFuture<Void> completableFuture) {
      try {
        return completableFuture.get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public WorkflowInboundCallsInterceptor interceptWorkflow(
        final WorkflowInboundCallsInterceptor next) {
      return new WorkflowInboundCallsInterceptorBase(next) {
        @Override
        public UpdateOutput executeUpdate(final UpdateInput input) {
          if (input.getUpdateName().equals("createTask")
              && Workflow.getInfo()
                  .getWorkflowType()
                  .equals(WorkflowTaskManager.class.getSimpleName())) {
            createTaskInvocations++;
            if (createTaskInvocations == 2) {
              waitUntilTwoInvocationsOfCreateTask.complete(null);
            }

            if (createTaskInvocations == 3) {
              waitUntilThreeInvocationsOfCreateTask.complete(null);
            }
          }

          return super.executeUpdate(input);
        }
      };
    }
  }
}
