package io.temporal.samples.hello;

import io.temporal.client.*;
import io.temporal.common.converter.EncodedValues;
import io.temporal.failure.ApplicationFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.DynamicWorkflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class TestFailedWorkflowExceptionTypes {
  private static final String TASK_QUEUE = "default";
  private static final String COMMON_WORKFLOW_ID = "ThrowExceptionWorkflow";
  private static final String DYNAMIC_WORKFLOW_ID = "ThrowExceptionDynamicWorkflow";

  @WorkflowInterface
  public interface ThrowExceptionWorkflow {
    @WorkflowMethod
    void execute();
  }

  public static class ThrowExceptionWorkflowImpl implements ThrowExceptionWorkflow {
    @Override
    public void execute() {
      throw new RuntimeException("xxx");
    }
  }

  public static class ThrowExceptionDynamicWorkflow implements DynamicWorkflow {
    @Override
    public Object execute(EncodedValues args) {
      throw ApplicationFailure.newFailure("xxx", "", "");
    }
  }

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClientOptions clientOptions =
        WorkflowClientOptions.newBuilder().setNamespace("default").build();
    WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);

    WorkflowImplementationOptions workflowImplementationOptions =
        WorkflowImplementationOptions.newBuilder()
            .setFailWorkflowExceptionTypes(RuntimeException.class)
            .build();

    worker.registerWorkflowImplementationTypes(
        workflowImplementationOptions,
        ThrowExceptionWorkflowImpl.class,
        ThrowExceptionDynamicWorkflow.class);
    factory.start();

    // common
    try {
      WorkflowOptions options =
          WorkflowOptions.newBuilder()
              .setTaskQueue(TASK_QUEUE)
              .setWorkflowId(COMMON_WORKFLOW_ID)
              .build();

      client.newWorkflowStub(ThrowExceptionWorkflow.class, options).execute();
    } catch (WorkflowException e) {
      System.out.println("COMMON:" + e.getCause().getMessage());
    }
    // dynamic
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setWorkflowId(DYNAMIC_WORKFLOW_ID)
            .setTaskQueue(TASK_QUEUE)
            .build();
    WorkflowStub workflowStub = client.newUntypedWorkflowStub("abc", workflowOptions);

    try {
      workflowStub.start();
      workflowStub.getResult(Void.class);
    } catch (WorkflowException e) {
      System.out.println("DYNAMIC:" + e.getMessage());
    }
  }
}
