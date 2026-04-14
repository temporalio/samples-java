package io.temporal.samples.nexusmessaging.ondemandpattern.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.temporal.samples.nexusmessaging.ondemandpattern.service.Language;
import io.temporal.samples.nexusmessaging.ondemandpattern.service.NexusRemoteGreetingService;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.UpdateValidatorMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * A long-running "entity" workflow that backs the NexusRemoteGreetingService Nexus operations. The
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

  class GetLanguagesInput {
    private final boolean includeUnsupported;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GetLanguagesInput(@JsonProperty("includeUnsupported") boolean includeUnsupported) {
      this.includeUnsupported = includeUnsupported;
    }

    @JsonProperty("includeUnsupported")
    public boolean isIncludeUnsupported() {
      return includeUnsupported;
    }
  }

  class SetLanguageInput {
    private final Language language;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SetLanguageInput(@JsonProperty("language") Language language) {
      this.language = language;
    }

    @JsonProperty("language")
    public Language getLanguage() {
      return language;
    }
  }

  @WorkflowMethod
  String run();

  // Returns the languages currently supported by the workflow.
  @QueryMethod
  NexusRemoteGreetingService.GetLanguagesOutput getLanguages(GetLanguagesInput input);

  // Returns the currently active language.
  @QueryMethod
  Language getLanguage();

  // Approves the workflow, allowing it to complete.
  @SignalMethod
  void approve(ApproveInput input);

  // Changes the active language synchronously (only supports languages already in the greetings
  // map).
  @UpdateMethod
  Language setLanguage(SetLanguageInput input);

  @UpdateValidatorMethod(updateName = "setLanguage")
  void validateSetLanguage(SetLanguageInput input);

  // Changes the active language, calling an activity to fetch a greeting for new languages.
  @UpdateMethod
  Language setLanguageUsingActivity(SetLanguageInput input);
}
