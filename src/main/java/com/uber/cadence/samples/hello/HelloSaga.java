package com.uber.cadence.samples.hello;

import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.*;

import java.time.Duration;

import static com.uber.cadence.samples.common.SampleConstants.DOMAIN;

/**
 * Demonstrates implementing saga transaction and compensation logic using Cadence.
 */
public class HelloSaga {
  static final String TASK_LIST = "HelloSaga";

  public interface ChildWorkflowOperation {
    @WorkflowMethod
    void execute(int amount);
  }

  public static class ChildWorkflowOperationImpl implements ChildWorkflowOperation {
    ActivityOperation activity = Workflow.newActivityStub(ActivityOperation.class);

    public void execute(int amount) {
      activity.execute(amount);
    }
  }

  public interface ChildWorkflowCompensation {
    @WorkflowMethod
    void compensate(int amount);
  }

  public static class ChildWorkflowCompensationImpl implements ChildWorkflowCompensation {
    ActivityOperation activity = Workflow.newActivityStub(ActivityOperation.class);

    public void compensate(int amount) {
      activity.compensate(amount);
    }
  }

  public interface ActivityOperation {
    @ActivityMethod(scheduleToCloseTimeoutSeconds = 2)
    void execute(int amount);

    @ActivityMethod(scheduleToCloseTimeoutSeconds = 2)
    void compensate(int amount);
  }

  public static class ActivityOperationImpl implements ActivityOperation {

    public void execute(int amount) {
      System.out.println("ActivityOperationImpl.execute() is called with amount " + amount);
    }

    public void compensate(int amount) {
      System.out.println("ActivityCompensationImpl.compensate() is called with amount " + amount);
    }
  }

  public interface SagaWorkflow {
    /**
     * Main saga workflow.
     * Here we execute activity operation twice (first from a child workflow, second directly using
     * activity stub), add three compensation functions, and then throws some exception in workflow code.
     * When we catch the exception, saga.compensate will run the compensation functions according
     * to the policy specified in SagaOptions.
     */
    @WorkflowMethod
    void execute();
  }

  public static class SagaWorkflowImpl implements SagaWorkflow {
    ActivityOperation activity = Workflow.newActivityStub(ActivityOperation.class);

    @Override
    public void execute() {
      Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
      try {
        ChildWorkflowOperation op1 = Workflow.newChildWorkflowStub(ChildWorkflowOperation.class);
        op1.execute(10);
        ChildWorkflowCompensation c1 = Workflow.newChildWorkflowStub(ChildWorkflowCompensation.class);
        saga.addCompensation(c1::compensate, -10);

        Promise<Void> result = Async.procedure(activity::execute, 20);
        result.get();
        saga.addCompensation(activity::compensate, -20);

        // The following is just to demonstrate the ability of supplying arbitrary lambda as a
        // saga compensation function. In production code please always use Workflow.getLogger
        // to log messages in workflow code.
        saga.addCompensation(() -> System.out.println("Other compensation logic in main workflow."));
        throw new RuntimeException("some error");

      } catch (Exception e) {
        saga.compensate();
      }
    }
  }

  public static void main(String[] args) {
    // Start a worker that hosts the workflow implementation.
    Worker.Factory factory = new Worker.Factory(DOMAIN);
    Worker worker = factory.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(
        HelloSaga.SagaWorkflowImpl.class,
        HelloSaga.ChildWorkflowOperationImpl.class,
        HelloSaga.ChildWorkflowCompensationImpl.class);
    worker.registerActivitiesImplementations(new ActivityOperationImpl());
    factory.start();

    // Start a workflow execution. Usually this is done from another program.
    WorkflowClient workflowClient = WorkflowClient.newInstance(DOMAIN);
    // Get a workflow stub using the same task list the worker uses.
    WorkflowOptions workflowOptions =
        new WorkflowOptions.Builder()
            .setTaskList(TASK_LIST)
            .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
            .build();
    HelloSaga.SagaWorkflow workflow =
        workflowClient.newWorkflowStub(HelloSaga.SagaWorkflow.class, workflowOptions);
    workflow.execute();
    System.exit(0);
  }
}
