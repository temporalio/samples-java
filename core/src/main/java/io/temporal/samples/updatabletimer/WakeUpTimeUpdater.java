package io.temporal.samples.updatabletimer;

import static io.temporal.samples.updatabletimer.DynamicSleepWorkflowWorker.DYNAMIC_SLEEP_WORKFLOW_ID;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WakeUpTimeUpdater {

  private static final Logger logger = LoggerFactory.getLogger(WakeUpTimeUpdater.class);

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    // Create a stub that points to an existing workflow with the given ID
    DynamicSleepWorkflow workflow =
        client.newWorkflowStub(DynamicSleepWorkflow.class, DYNAMIC_SLEEP_WORKFLOW_ID);

    // signal workflow about the wake up time change
    workflow.updateWakeUpTime(System.currentTimeMillis() + 10000);
    logger.info("Updated wake up time to 10 seconds from now");
  }
}
