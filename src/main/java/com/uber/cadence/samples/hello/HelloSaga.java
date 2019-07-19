package com.uber.cadence.samples.hello;

import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.uber.cadence.samples.common.SampleConstants.DOMAIN;

/**
 * Demonstrates implementing saga transaction and compensation logic using Cadence.
 */
public class HelloSaga {
  static final String TASK_LIST = "HelloSaga";
  static final List<String> transactions = new ArrayList<>();

  public interface ChildWorkflowOperation {
    @WorkflowMethod
    void execute(int amount);
  }

  public static class ChildWorkflowOperationImpl implements ChildWorkflowOperation {
    Logger log = Workflow.getLogger(ChildWorkflowOperationImpl.class);

    public void execute(int amount) {
      log.info("ChildWorkflowOperationImpl.execute() is called.");
      transactions.add("child workflow execution: " + amount);
    }
  }

  public interface ChildWorkflowCompensation {
    @WorkflowMethod
    void compensate(int amount);
  }

  public static class ChildWorkflowCompensationImpl implements ChildWorkflowCompensation {
    Logger log = Workflow.getLogger(ChildWorkflowCompensationImpl.class);

    public void compensate(int amount) {
      log.info("ChildWorkflowCompensationImpl.compensate() is called.");
      transactions.add("child workflow compensation: " + amount);
    }
  }

  public interface ActivityOperation {
    @ActivityMethod(scheduleToCloseTimeoutSeconds = 2)
    void execute(int amount);
  }

  public static class ActivityOperationImpl implements ActivityOperation {
    Logger log = LoggerFactory.getLogger(ActivityOperationImpl.class);

    public void execute(int amount) {
      log.info("ActivityOperationImpl.execute() is called.");
      transactions.add("activity execution: " + amount);
    }
  }

  public interface ActivityCompensation {
    @ActivityMethod(scheduleToCloseTimeoutSeconds = 2)
    void compensate(int amount);
  }

  public static class ActivityCompensationImpl implements ActivityCompensation {
    Logger log = LoggerFactory.getLogger(ActivityCompensationImpl.class);

    public void compensate(int amount) {
      log.info("ActivityCompensationImpl.execute() is called.");
      transactions.add("activity compensation: " + amount);
    }
  }

  public interface SagaWorkflow {
    /**
     * Main saga workflow.
     */
    @WorkflowMethod
    List<String> execute();
  }

  public static class SagaWorkflowImpl implements SagaWorkflow {
    @Override
    public List<String> execute() {
      Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
      try {
        ChildWorkflowOperation op1 = Workflow.newChildWorkflowStub(ChildWorkflowOperation.class);
        op1.execute(10);
        ChildWorkflowCompensation c1 = Workflow.newChildWorkflowStub(ChildWorkflowCompensation.class);
        saga.addCompensation(c1::compensate, -10);

        ActivityOperation op2 = Workflow.newActivityStub(ActivityOperation.class);
        Promise<Void> result = Async.procedure(op2::execute, 20);
        result.get();
        ActivityCompensation c2 = Workflow.newActivityStub(ActivityCompensation.class);
        saga.addCompensation(c2::compensate, -20);

        transactions.add("main workflow: " + 30);
        saga.addCompensation(()->transactions.add("main workflow compensation: " + -30));

        throw new RuntimeException("some error");

      } catch (Exception e) {
        saga.compensate();
      }

      return transactions;
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
    worker.registerActivitiesImplementations(new ActivityOperationImpl(), new ActivityCompensationImpl());
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
    System.out.println(workflow.execute());
    System.exit(0);
  }
}
