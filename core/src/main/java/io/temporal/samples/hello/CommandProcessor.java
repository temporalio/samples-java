package io.temporal.samples.hello;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.CompletablePromise;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class CommandProcessor {
  static final String TASK_QUEUE = "MyTaskQueue";
  static final String WORKFLOW_ID = "MyWorkflowId";

  @WorkflowInterface
  public interface CommandProcessorWorkflow {
    @WorkflowMethod
    String startProcessing();

    @UpdateMethod
    String submitCommand(int command);
  }

  @ActivityInterface
  public interface MyActivities {
    String processCommand(int command);
  }

  public static class CommandProcessorWorkflowImpl implements CommandProcessorWorkflow {

    private static class QueuedCommand {
      public int command;
      public CompletablePromise<String> promise;

      public QueuedCommand(int command, CompletablePromise<String> promise) {
        this.command = command;
        this.promise = promise;
      }
    }

    private ArrayList<QueuedCommand> commandQueue;
    private boolean done;

    public CommandProcessorWorkflowImpl() {
      this.commandQueue = new ArrayList<>();
      this.done = false;
    }

    private final MyActivities activities =
        Workflow.newActivityStub(
            MyActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public String startProcessing() {
      while (true) {
        Workflow.await(() -> this.commandQueue.size() > 0 || this.done);
        if (this.done && this.commandQueue.size() == 0) {
          return "done";
        }
        QueuedCommand queuedCommand = this.commandQueue.remove(0);
        String result = activities.processCommand(queuedCommand.command);
        queuedCommand.promise.complete(result);
      }
    }

    @Override
    public String submitCommand(int command) {
      if (command < 0) {
        this.done = true;
        return "stopping workflow";
      }
      CompletablePromise<String> promise = Workflow.newPromise();
      QueuedCommand queuedCommand = new QueuedCommand(command, promise);
      this.commandQueue.add(queuedCommand);
      return queuedCommand.promise.get();
    }
  }

  static class MyActivitiesImpl implements MyActivities {
    @Override
    public String processCommand(int commandNum) {
      try {
        // Earlier commands are slower, so we must serialize if they are to complete in order of
        // receipt.
        Thread.sleep(1000L * (3 - commandNum));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      String result = commandNum + " [processed]";
      System.out.println(result);
      return result;
    }
  }

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(CommandProcessorWorkflowImpl.class);
    worker.registerActivitiesImplementations(new MyActivitiesImpl());
    factory.start();
    CommandProcessorWorkflow commandProcessor =
        client.newWorkflowStub(
            CommandProcessorWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowIdReusePolicy(
                    WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_TERMINATE_IF_RUNNING)
                .build());

    WorkflowStub untypedWorkflowStub = WorkflowStub.fromTyped(commandProcessor);

    WorkflowClient.start(commandProcessor::startProcessing);

    CompletableFuture.allOf(
            untypedWorkflowStub.startUpdate("submitCommand", String.class, 1).getResultAsync(),
            untypedWorkflowStub.startUpdate("submitCommand", String.class, 2).getResultAsync())
        .join();
    commandProcessor.submitCommand(-1);
    untypedWorkflowStub.getResult(String.class);
    System.exit(0);
  }
}
