package io.temporal.samples.nexus_messaging.ondemandpattern.handler;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.nexus_messaging.ondemandpattern.service.Language;
import io.temporal.samples.nexus_messaging.ondemandpattern.service.NexusRemoteGreetingService;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class GreetingWorkflowImpl implements GreetingWorkflow {

  private static final Logger logger1 = Workflow.getLogger(GreetingWorkflowImpl.class);
  private boolean approvedForRelease = false;
  private final Map<Language, String> greetings = new EnumMap<>(Language.class);
  private Language language = Language.ENGLISH;

  private static final Logger logger = logger1;

  private final GreetingActivity greetingActivity =
      Workflow.newActivityStub(
          GreetingActivity.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  public GreetingWorkflowImpl() {
    greetings.put(Language.CHINESE, "你好，世界");
    greetings.put(Language.ENGLISH, "Hello, world");
  }

  @Override
  public String run() {
    // Wait until approved and all in-flight update handlers have finished.
    Workflow.await(() -> approvedForRelease && Workflow.isEveryHandlerFinished());
    return greetings.get(language);
  }

  @Override
  public NexusRemoteGreetingService.GetLanguagesOutput getLanguages(
      GreetingWorkflow.GetLanguagesInput input) {
    List<Language> result;
    if (input.isIncludeUnsupported()) {
      result = new ArrayList<>(Arrays.asList(Language.values()));
    } else {
      result = new ArrayList<>(greetings.keySet());
    }
    Collections.sort(result);
    return new NexusRemoteGreetingService.GetLanguagesOutput(result);
  }

  @Override
  public Language getLanguage() {
    return language;
  }

  @Override
  public void approve(ApproveInput input) {
    logger.info("Approval signal received");
    approvedForRelease = true;
  }

  @Override
  public Language setLanguage(GreetingWorkflow.SetLanguageInput input) {
    logger.info("setLanguage update received");
    Language previous = language;
    language = input.getLanguage();
    return previous;
  }

  @Override
  public void validateSetLanguage(GreetingWorkflow.SetLanguageInput input) {
    logger.info("validateSetLanguage called");
    if (!greetings.containsKey(input.getLanguage())) {
      throw new IllegalArgumentException(input.getLanguage().name() + " is not supported");
    }
  }

  @Override
  public Language setLanguageUsingActivity(GreetingWorkflow.SetLanguageInput input) {
    if (!greetings.containsKey(input.getLanguage())) {
      String greeting = greetingActivity.callGreetingService(input.getLanguage());
      if (greeting == null) {
        throw ApplicationFailure.newFailure(
            "Greeting service does not support " + input.getLanguage().name(),
            "UnsupportedLanguage");
      }
      greetings.put(input.getLanguage(), greeting);
    }
    Language previous = language;
    language = input.getLanguage();
    return previous;
  }
}
