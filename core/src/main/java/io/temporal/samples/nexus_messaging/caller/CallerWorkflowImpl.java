package io.temporal.samples.nexus_messaging.caller;

import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.nexus_messaging.caller_remote.CallerRemoteWorkflowImpl;
import io.temporal.samples.nexus_messaging.service.Language;
import io.temporal.samples.nexus_messaging.service.NexusGreetingService;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallerWorkflowImpl implements CallerWorkflow {

  private static final Logger logger = LoggerFactory.getLogger(CallerRemoteWorkflowImpl.class);

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
  public List<String> run() {
    List<String> log = new ArrayList<>();

    // 👉 Call a Nexus operation backed by a query against the entity workflow.
    NexusGreetingService.GetLanguagesOutput languagesOutput =
        greetingService.getLanguages(new NexusGreetingService.GetLanguagesInput(false));
    log.add("supported languages: " + languagesOutput.getLanguages());

    // 👉 Call a Nexus operation backed by an update against the entity workflow.
    Language previousLanguage =
        greetingService.setLanguage(new NexusGreetingService.SetLanguageInput(Language.ARABIC));
    logger.info("Language changed from {} to {}", previousLanguage, Language.ARABIC);

    // 👉 Call a Nexus operation backed by a query to confirm the language change.
    Language currentLanguage =
        greetingService.getLanguage(new NexusGreetingService.GetLanguageInput());
    if (currentLanguage != Language.ARABIC) {
      throw ApplicationFailure.newFailure(
          "expected language ARABIC, got " + currentLanguage, "AssertionError");
    }

    log.add("language changed: " + previousLanguage.name() + " -> " + Language.ARABIC.name());

    // 👉 Call a Nexus operation backed by a signal against the entity workflow.
    greetingService.approve(new NexusGreetingService.ApproveInput("caller"));
    log.add("workflow approved");

    return log;
  }
}
