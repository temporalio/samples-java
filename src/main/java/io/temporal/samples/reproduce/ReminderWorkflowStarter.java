package io.temporal.samples.reproduce;

import io.temporal.client.BatchRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public final class ReminderWorkflowStarter {

  private static final String TASK_QUEUE = "ReminderWorkflowQueue";

  private static final String WORKFLOW_ID = "ReminderWorkflow";

  public static void main(String[] args) throws InterruptedException {
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactory factory1 = startWorkerFactory(client);

    ReminderWorkflow stub = newWorkflowStub(client);
    WorkflowClient.start(stub::start);

    Thread.sleep(5000L);

    factory1.shutdown();
    factory1.awaitTermination(10, TimeUnit.SECONDS);

    WorkerFactory factory2 = startWorkerFactory(client);

    scheduleReminder(client, Instant.now().plus(60, ChronoUnit.SECONDS), "Reminder 2");
    scheduleReminder(client, Instant.now().plus(30, ChronoUnit.SECONDS), "Reminder 3");
  }

  private static WorkerFactory startWorkerFactory(WorkflowClient client) {
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(ReminderWorkflowImpl.class);
    factory.start();
    return factory;
  }

  private static void scheduleReminder(
      WorkflowClient client, Instant reminderTime, String reminderText) {
    ReminderWorkflow stub = newWorkflowStub(client);
    BatchRequest request = client.newSignalWithStartRequest();
    request.add(stub::start);
    request.add(stub::scheduleReminder, new ScheduleReminderSignal(reminderTime, reminderText));
    client.signalWithStart(request);
  }

  private static ReminderWorkflow newWorkflowStub(WorkflowClient client) {
    return client.newWorkflowStub(
        ReminderWorkflow.class,
        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(WORKFLOW_ID).build());
  }
}
