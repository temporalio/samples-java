package io.temporal.samples.nexusmessaging.ondemandpattern.caller;

import io.temporal.samples.nexusmessaging.ondemandpattern.service.Language;
import io.temporal.samples.nexusmessaging.ondemandpattern.service.NexusRemoteGreetingService;
import io.temporal.workflow.NexusOperationHandle;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

public class CallerRemoteWorkflowImpl implements CallerRemoteWorkflow {

  private static final Logger logger = Workflow.getLogger(CallerRemoteWorkflowImpl.class);

  // This is going to create two workflows and send messages to them.
  // We need to have an ID to differentiate so that Nexus knows how to name
  // a workflow and then how to know the correct destination workflow.
  private static final String REMOTE_WORKFLOW_ONE = "UserId One";
  private static final String REMOTE_WORKFLOW_TWO = "UserId Two";

  NexusRemoteGreetingService greetingRemoteServiceOne =
      Workflow.newNexusServiceStub(
          NexusRemoteGreetingService.class,
          NexusServiceOptions.newBuilder()
              .setOperationOptions(
                  NexusOperationOptions.newBuilder()
                      .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                      .build())
              .build());
  NexusRemoteGreetingService greetingRemoteServiceTwo =
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
    // Messages in the log array are passed back to the caller who will then log them to report what
    // is happening.
    // The same message is also logged for demo purposes, so that things are visible in the caller
    // workflow output.
    List<String> log = new ArrayList<>();

    // Each call is performed twice in this example. This assumes there are two users we want
    // to process. The first call starts two workflows, one for each user.
    // Subsequent calls perform different actions between the two users.
    // There are examples for each of the three messaging types -
    // update, query, then signal.

    // This is an Async Nexus operation — starts a workflow on the handler and returns a handle.
    // Unlike the sync operations below (getLanguages, setLanguage, etc.), this does not block
    // until the workflow completes. It is backed by WorkflowRunOperation on the handler side.
    NexusOperationHandle<String> handleOne =
        Workflow.startNexusOperation(
            greetingRemoteServiceOne::runFromRemote,
            new NexusRemoteGreetingService.RunFromRemoteInput(REMOTE_WORKFLOW_ONE));
    // Wait for the operation to be started (workflow is now running on the handler).
    handleOne.getExecution().get();
    log.add("started remote greeting workflow: " + REMOTE_WORKFLOW_ONE);
    logger.info("started remote greeting workflow {}", REMOTE_WORKFLOW_ONE);

    NexusOperationHandle<String> handleTwo =
        Workflow.startNexusOperation(
            greetingRemoteServiceTwo::runFromRemote,
            new NexusRemoteGreetingService.RunFromRemoteInput(REMOTE_WORKFLOW_TWO));
    // Wait for the operation to be started (workflow is now running on the handler).
    handleTwo.getExecution().get();
    log.add("started remote greeting workflow: " + REMOTE_WORKFLOW_TWO);
    logger.info("started remote greeting workflow {}", REMOTE_WORKFLOW_TWO);

    // Query the remote workflow for supported languages.
    NexusRemoteGreetingService.GetLanguagesOutput languagesOutput =
        greetingRemoteServiceOne.getLanguages(
            new NexusRemoteGreetingService.GetLanguagesInput(false, REMOTE_WORKFLOW_ONE));
    log.add(
        "Supported languages for " + REMOTE_WORKFLOW_ONE + ": " + languagesOutput.getLanguages());
    logger.info(
        "supported languages are {} for workflow {}",
        languagesOutput.getLanguages(),
        REMOTE_WORKFLOW_ONE);

    languagesOutput =
        greetingRemoteServiceTwo.getLanguages(
            new NexusRemoteGreetingService.GetLanguagesInput(false, REMOTE_WORKFLOW_TWO));
    log.add(
        "Supported languages for " + REMOTE_WORKFLOW_TWO + ": " + languagesOutput.getLanguages());
    logger.info(
        "supported languages are {} for workflow {}",
        languagesOutput.getLanguages(),
        REMOTE_WORKFLOW_TWO);

    // Update the language on the remote workflow.
    Language previousLanguageOne =
        greetingRemoteServiceOne.setLanguage(
            new NexusRemoteGreetingService.SetLanguageInput(Language.ARABIC, REMOTE_WORKFLOW_ONE));

    Language previousLanguageTwo =
        greetingRemoteServiceTwo.setLanguage(
            new NexusRemoteGreetingService.SetLanguageInput(Language.HINDI, REMOTE_WORKFLOW_TWO));

    // Confirm the change by querying.
    Language currentLanguage =
        greetingRemoteServiceOne.getLanguage(
            new NexusRemoteGreetingService.GetLanguageInput(REMOTE_WORKFLOW_ONE));
    log.add(
        REMOTE_WORKFLOW_ONE
            + " changed language: "
            + previousLanguageOne.name()
            + " -> "
            + currentLanguage.name());
    logger.info(
        "Language changed from {} to {} for workflow {}",
        previousLanguageOne,
        currentLanguage,
        REMOTE_WORKFLOW_ONE);

    currentLanguage =
        greetingRemoteServiceTwo.getLanguage(
            new NexusRemoteGreetingService.GetLanguageInput(REMOTE_WORKFLOW_TWO));
    log.add(
        REMOTE_WORKFLOW_TWO
            + " changed language: "
            + previousLanguageTwo.name()
            + " -> "
            + currentLanguage.name());
    logger.info(
        "Language changed from {} to {} for workflow {}",
        previousLanguageTwo,
        currentLanguage,
        REMOTE_WORKFLOW_TWO);

    // Approve the remote workflow so it can complete.
    greetingRemoteServiceOne.approve(
        new NexusRemoteGreetingService.ApproveInput("remote-caller", REMOTE_WORKFLOW_ONE));
    greetingRemoteServiceTwo.approve(
        new NexusRemoteGreetingService.ApproveInput("remote-caller", REMOTE_WORKFLOW_TWO));
    log.add("Workflows approved");

    // Wait for the remote workflow to finish and return its result.
    String result = handleOne.getResult().get();
    log.add("Workflow one result: " + result);

    result = handleTwo.getResult().get();
    log.add("Workflow two result: " + result);
    return log;
  }
}
