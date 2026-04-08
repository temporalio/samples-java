package io.temporal.samples.nexus_sync_operations.handler;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.nexus_sync_operations.service.Language;
import io.temporal.samples.nexus_sync_operations.service.NexusGreetingService;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowLock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreetingWorkflowImpl implements GreetingWorkflow {

  private boolean approvedForRelease = false;
  private final Map<Language, String> greetings = new EnumMap<>(Language.class);
  private Language language = Language.ENGLISH;

  private static final Logger logger = LoggerFactory.getLogger(HandlerWorker.class);

  // Used to serialize concurrent setLanguageUsingActivity calls so that only one activity runs at
  // a time per update handler execution.
  private final WorkflowLock lock = Workflow.newWorkflowLock();

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
  public NexusGreetingService.GetLanguagesOutput getLanguages(
      NexusGreetingService.GetLanguagesInput input) {
    List<Language> result;
    if (input.isIncludeUnsupported()) {
      result = new ArrayList<>(Arrays.asList(Language.values()));
    } else {
      result = new ArrayList<>(greetings.keySet());
    }
    Collections.sort(result);
    return new NexusGreetingService.GetLanguagesOutput(result);
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
  public Language setLanguage(NexusGreetingService.SetLanguageInput input) {
    logger.info("setLanguage update received");
    Language previous = language;
    language = input.getLanguage();
    return previous;
  }

  @Override
  public void validateSetLanguage(NexusGreetingService.SetLanguageInput input) {
    logger.info("validateSetLanguage called");
    if (!greetings.containsKey(input.getLanguage())) {
      throw new IllegalArgumentException(input.getLanguage().name() + " is not supported");
    }
  }

  @Override
  public Language setLanguageUsingActivity(NexusGreetingService.SetLanguageInput input) {
    if (!greetings.containsKey(input.getLanguage())) {
      // Use a lock so that if this handler is called concurrently, each call executes its activity
      // only after the previous one has completed. This ensures updates are processed in order.
      lock.lock();
      try {
        String greeting = greetingActivity.callGreetingService(input.getLanguage());
        if (greeting == null) {
          throw ApplicationFailure.newFailure(
              "Greeting service does not support " + input.getLanguage().name(),
              "UnsupportedLanguage");
        }
        greetings.put(input.getLanguage(), greeting);
      } finally {
        lock.unlock();
      }
    }
    Language previous = language;
    language = input.getLanguage();
    return previous;
  }
}
