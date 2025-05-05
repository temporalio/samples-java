

package io.temporal.samples.updatabletimer;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicSleepWorkflowWorker {

  static final String TASK_QUEUE = "TimerUpdate";

  private static final Logger logger = LoggerFactory.getLogger(DynamicSleepWorkflowWorker.class);

  /** Create just one workflow instance for the sake of the sample. */
  static final String DYNAMIC_SLEEP_WORKFLOW_ID = "DynamicSleepWorkflow";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    final Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(DynamicSleepWorkflowImpl.class);
    factory.start();
    logger.info("Worker started for task queue: " + TASK_QUEUE);
  }
}
