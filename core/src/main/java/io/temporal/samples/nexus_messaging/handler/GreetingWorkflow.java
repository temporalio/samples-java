package io.temporal.samples.nexus_messaging.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.temporal.samples.nexus_messaging.service.Language;
import io.temporal.samples.nexus_messaging.service.NexusGreetingService;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.UpdateValidatorMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * A long-running "entity" workflow that backs the NexusGreetingService Nexus operations. The
 * workflow exposes queries, an update, and a signal. These are private implementation details of
 * the Nexus service: the caller only interacts via Nexus operations.
 */
@WorkflowInterface
public interface GreetingWorkflow {

  class ApproveInput {
    private final String name;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ApproveInput(@JsonProperty("name") String name) {
      this.name = name;
    }

    @JsonProperty("name")
    public String getName() {
      return name;
    }
  }

  @WorkflowMethod
  String run();

  // Returns the languages currently supported by the workflow.
  @QueryMethod
  NexusGreetingService.GetLanguagesOutput getLanguages(
      NexusGreetingService.GetLanguagesInput input);

  // Returns the currently active language.
  @QueryMethod
  Language getLanguage();

  // Approves the workflow, allowing it to complete.
  @SignalMethod
  void approve(ApproveInput input);

  // Changes the active language synchronously (only supports languages already in the greetings
  // map).
  @UpdateMethod
  Language setLanguage(NexusGreetingService.SetLanguageInput input);

  @UpdateValidatorMethod(updateName = "setLanguage")
  void validateSetLanguage(NexusGreetingService.SetLanguageInput input);

  // Changes the active language, calling an activity to fetch a greeting for new languages.
  @UpdateMethod
  Language setLanguageUsingActivity(NexusGreetingService.SetLanguageInput input);
}
