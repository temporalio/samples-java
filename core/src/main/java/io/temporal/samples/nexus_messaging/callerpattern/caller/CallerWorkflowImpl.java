package io.temporal.samples.nexus_messaging.callerpattern.caller;

import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.nexus_messaging.callerpattern.service.Language;
import io.temporal.samples.nexus_messaging.callerpattern.service.NexusGreetingService;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

public class CallerWorkflowImpl implements CallerWorkflow {

  private static final Logger logger = Workflow.getLogger(CallerWorkflowImpl.class);

  // The endpoint is configured at the worker level in CallerWorker; only operation options are
  // set here.
  NexusGreetingService greetingService =
      Workflow.newNexusServiceStub(
          NexusGreetingService.class,
          NexusServiceOptions.newBuilder()
              .setOperationOptions(
                  NexusOperationOptions.newBuilder()
                      .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                      .build())
              .build());

  @Override
  public List<String> run(String userId) {

    // Messages in the log array are passed back to the caller who will then log them to report what
    // is happening.
    // The same message is also logged for demo purposes, so that things are visible in the caller
    // workflow output.
    List<String> log = new ArrayList<>();

    // Call a Nexus operation backed by a query against the entity workflow.
    // The workflow must already be running on the handler, otherwise you will
    // get an error saying the workflow has already terminated.
    NexusGreetingService.GetLanguagesOutput languagesOutput =
        greetingService.getLanguages(new NexusGreetingService.GetLanguagesInput(false, userId));
    log.add("Supported languages: " + languagesOutput.getLanguages());
    logger.info("Supported languages: {}", languagesOutput.getLanguages());

    // Following are examples for each of the three messaging types -
    // update, query, then signal.

    // Call a Nexus operation backed by an update against the entity workflow.
    Language previousLanguage =
        greetingService.setLanguage(
            new NexusGreetingService.SetLanguageInput(Language.ARABIC, userId));

    // Call a Nexus operation backed by a query to confirm the language change.
    Language currentLanguage =
        greetingService.getLanguage(new NexusGreetingService.GetLanguageInput(userId));
    if (currentLanguage != Language.ARABIC) {
      throw ApplicationFailure.newFailure(
          "Expected language ARABIC, got " + currentLanguage, "AssertionError");
    }

    log.add("Language changed: " + previousLanguage.name() + " -> " + Language.ARABIC.name());
    logger.info("Language changed from {} to {}", previousLanguage, Language.ARABIC);

    // Call a Nexus operation backed by a signal against the entity workflow.
    greetingService.approve(new NexusGreetingService.ApproveInput("caller", userId));
    log.add("Workflow approved");
    logger.info("Workflow approved");

    return log;
  }
}
