package io.temporal.samples.updatabletimer;

import static io.temporal.samples.updatabletimer.DynamicSleepWorkflowWorker.DYNAMIC_SLEEP_WORKFLOW_ID;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WakeUpTimeUpdater {

  private static final Logger logger = LoggerFactory.getLogger(WakeUpTimeUpdater.class);

  public static void main(String[] args) {
    // Load configuration from environment and files
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

    // Create a stub that points to an existing workflow with the given ID
    DynamicSleepWorkflow workflow =
        client.newWorkflowStub(DynamicSleepWorkflow.class, DYNAMIC_SLEEP_WORKFLOW_ID);

    // signal workflow about the wake up time change
    workflow.updateWakeUpTime(System.currentTimeMillis() + 10000);
    logger.info("Updated wake up time to 10 seconds from now");
  }
}
