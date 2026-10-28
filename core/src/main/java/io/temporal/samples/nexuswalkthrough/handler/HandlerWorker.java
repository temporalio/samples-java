package io.temporal.samples.nexuswalkthrough.handler;

import io.temporal.client.WorkflowClient;
import io.temporal.samples.nexuswalkthrough.options.ClientOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * WALKTHROUGH STEP 4 - Run the Worker.
 *
 * <p>One Worker hosts the Nexus Service implementation, the Workflow implementation, and the
 * Activity implementations. Its Task Queue has to match the Task Queue the Nexus Endpoint targets,
 * which is created in step 5.
 *
 * <p>A Worker registering a Nexus Service does not have to be the same Worker that runs the backing
 * Workflow. Splitting them is a normal choice for larger deployments; this sample keeps one Worker
 * so the moving parts stay visible.
 */
public class HandlerWorker {

  public static final String DEFAULT_TASK_QUEUE_NAME = "approval-handler-task-queue";

  // @@@SNIPSTART samples-java-nexus-walkthrough-handler-worker
  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient(args);

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(DEFAULT_TASK_QUEUE_NAME);

    worker.registerWorkflowImplementationTypes(ApprovalWorkflowImpl.class);
    worker.registerActivitiesImplementations(new ApprovalActivitiesImpl());
    worker.registerNexusServiceImplementation(new ApprovalServiceImpl());

    factory.start();
  }
  // @@@SNIPEND
}
