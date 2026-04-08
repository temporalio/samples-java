package io.temporal.samples.nexus_messaging.caller_remote;

import io.temporal.samples.nexus_messaging.service.Language;
import io.temporal.samples.nexus_messaging.service.NexusGreetingService;
import io.temporal.samples.nexus_messaging.service.NexusRemoteGreetingService;
import io.temporal.workflow.NexusOperationHandle;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallerRemoteWorkflowImpl implements CallerRemoteWorkflow {

  private static final Logger logger = LoggerFactory.getLogger(CallerRemoteWorkflowImpl.class);

  private static final String REMOTE_WORKFLOW_ID = "nexus-sync-operations-remote-greeting-workflow";

  NexusRemoteGreetingService greetingRemoteService =
      Workflow.newNexusServiceStub(
          NexusRemoteGreetingService.class,
          NexusServiceOptions.newBuilder()
              .setOperationOptions(
                  NexusOperationOptions.newBuilder()
                      .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                      .build())
              .build());

  @Override
  public List<String> run() {
    List<String> log = new ArrayList<>();

    // Start a new GreetingWorkflow on the handler side via Nexus.
    NexusOperationHandle<String> handle =
        Workflow.startNexusOperation(
            greetingRemoteService::runFromRemote,
            new NexusRemoteGreetingService.RunFromRemoteInput(REMOTE_WORKFLOW_ID));
    // Wait for the operation to be started (workflow is now running on the handler).
    handle.getExecution().get();
    log.add("started remote greeting workflow: " + REMOTE_WORKFLOW_ID);

    // Query the remote workflow for supported languages.
    NexusGreetingService.GetLanguagesOutput languagesOutput =
        greetingRemoteService.getLanguages(
            new NexusRemoteGreetingService.GetLanguagesInput(false, REMOTE_WORKFLOW_ID));
    log.add("supported languages: " + languagesOutput.getLanguages());

    // Update the language on the remote workflow.
    Language previousLanguage =
        greetingRemoteService.setLanguage(
            new NexusRemoteGreetingService.SetLanguageInput(Language.ARABIC, REMOTE_WORKFLOW_ID));
    logger.info("Language changed from {}", previousLanguage);

    // Confirm the change by querying.
    Language currentLanguage =
        greetingRemoteService.getLanguage(
            new NexusRemoteGreetingService.GetLanguageInput(REMOTE_WORKFLOW_ID));
    log.add("language changed: " + previousLanguage.name() + " -> " + currentLanguage.name());

    // Approve the remote workflow so it can complete.
    greetingRemoteService.approve(
        new NexusRemoteGreetingService.ApproveInput("remote-caller", REMOTE_WORKFLOW_ID));
    log.add("workflow approved");

    // Wait for the remote workflow to finish and return its result.
    String result = handle.getResult().get();
    log.add("workflow result: " + result);

    return log;
  }
}
