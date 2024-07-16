package io.temporal.samples.custom_activity;

import io.temporal.activity.*;
import io.temporal.client.ActivityCompletionException;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
  static final String TASK_QUEUE = "CustomActivityTaskQueue";
  static final String WORKFLOW_ID = "workflow_id_" + UUID.randomUUID();

  public static void main(String[] args) {
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder().setTarget("127.0.0.1:7233").build());
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(CustomWorkflowImpl.class);
    worker.registerActivitiesImplementations(new CustomActivityImpl());
    factory.start();
    CustomWorkflow workflow =
        client.newWorkflowStub(
            CustomWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    String result = workflow.doSomeWork();
    System.out.println(result);
    try {
      // pause and make sure we dont have a "rogue thread" that is still executing...
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      System.out.println(e.getMessage());
    }
    System.exit(0);
  }

  @ActivityInterface
  public interface CustomActivity {
    @ActivityMethod
    String doSomeWork();
  }

  @WorkflowInterface
  public interface CustomWorkflow {
    @WorkflowMethod
    String doSomeWork();
  }

  // https://aozturk.medium.com/how-to-handle-uncaught-exceptions-in-java-abf819347906
  public static class CustomActivityImpl implements CustomActivity {

    @Override
    public String doSomeWork() {
      Instant exitAt = Instant.now().plus(Duration.ofHours(1));
      var executionContext = Activity.getExecutionContext();
      final ScheduledExecutorService scheduledExecutor =
          Executors.newSingleThreadScheduledExecutor();
      try {
        var unused =
            scheduledExecutor.scheduleAtFixedRate(
                () -> {
                  try {
                    System.out.println("sending heartbeat");
                    executionContext.heartbeat(1);
                  } catch (ActivityCompletionException e) {
                    System.out.println("activitycompletionexception " + e.getMessage());
                    throw e;
                  }
                },
                0,
                1,
                TimeUnit.SECONDS);
        while (Instant.now().isBefore(exitAt)) {
          sleep(Duration.ofSeconds(5));
        }
        return "Done";
      } finally {
        System.out.println("shutting down heartbeat thread");
        scheduledExecutor.shutdown();
      }
    }

    private void sleep(Duration duration) {
      try {
        System.out.println("Sleeping for " + duration);
        Thread.sleep(duration.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
  }

  public static class CustomWorkflowImpl implements CustomWorkflow {
    private final CustomActivity customActivity =
        Workflow.newActivityStub(
            CustomActivity.class,
            ActivityOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                .setHeartbeatTimeout(Duration.ofSeconds(2))
                .setStartToCloseTimeout(Duration.ofHours(1))
                .build());

    @Override
    public String doSomeWork() {
      List<Promise<String>> results = new ArrayList<>(1);

      CancellationScope scope =
          Workflow.newCancellationScope(
              () -> {
                results.add(Async.function(customActivity::doSomeWork));
              });
      scope.run();
      Workflow.sleep(3000);
      scope.cancel();
      System.out.println("failure " + results.get(0).getFailure().getMessage());
      //       String result = Promise.anyOf(results).get();

      return "Cancellation worked";
    }
  }
}
