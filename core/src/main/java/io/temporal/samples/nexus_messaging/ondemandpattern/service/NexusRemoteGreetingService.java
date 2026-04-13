package io.temporal.samples.nexus_messaging.ondemandpattern.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;
import java.util.List;

/**
 * Nexus service definition for the on-demand pattern. Every operation includes a {@code userId} so
 * the caller controls which workflow instance is targeted though that, while the Nexus service
 * converts that UserId into a WorkflowId. This also exposes a {@code runFromRemote} operation that
 * starts a new GreetingWorkflow.
 */
@Service
public interface NexusRemoteGreetingService {

  class RunFromRemoteInput {
    private final String userId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RunFromRemoteInput(@JsonProperty("userId") String userId) {
      this.userId = userId;
    }

    @JsonProperty("userId")
    public String getUserId() {
      return userId;
    }
  }

  class GetLanguagesInput {
    private final boolean includeUnsupported;
    private final String userId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GetLanguagesInput(
        @JsonProperty("includeUnsupported") boolean includeUnsupported,
        @JsonProperty("userId") String userId) {
      this.includeUnsupported = includeUnsupported;
      this.userId = userId;
    }

    @JsonProperty("includeUnsupported")
    public boolean isIncludeUnsupported() {
      return includeUnsupported;
    }

    @JsonProperty("userId")
    public String getUserId() {
      return userId;
    }
  }

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  class GetLanguageInput {
    private final String userId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GetLanguageInput(@JsonProperty("userId") String userId) {
      this.userId = userId;
    }

    @JsonProperty("userId")
    public String getUserId() {
      return userId;
    }
  }

  class SetLanguageInput {
    private final Language language;
    private final String userId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SetLanguageInput(
        @JsonProperty("language") Language language, @JsonProperty("userId") String userId) {
      this.language = language;
      this.userId = userId;
    }

    @JsonProperty("language")
    public Language getLanguage() {
      return language;
    }

    @JsonProperty("userId")
    public String getUserId() {
      return userId;
    }
  }

  class ApproveInput {
    private final String name;
    private final String userId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ApproveInput(@JsonProperty("name") String name, @JsonProperty("userId") String userId) {
      this.name = name;
      this.userId = userId;
    }

    @JsonProperty("name")
    public String getName() {
      return name;
    }

    @JsonProperty("userId")
    public String getUserId() {
      return userId;
    }
  }

  class GetLanguagesOutput {
    private final List<Language> languages;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GetLanguagesOutput(@JsonProperty("languages") List<Language> languages) {
      this.languages = languages;
    }

    @JsonProperty("languages")
    public List<Language> getLanguages() {
      return languages;
    }
  }

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  class ApproveOutput {
    @JsonCreator
    public ApproveOutput() {}
  }

  // Starts a new GreetingWorkflow for the given user ID. This is an asynchronous Nexus
  // operation: the caller receives a handle and can wait for the workflow to complete.
  @Operation
  String runFromRemote(RunFromRemoteInput input);

  // Returns the languages supported by the specified workflow.
  @Operation
  GetLanguagesOutput getLanguages(GetLanguagesInput input);

  // Returns the currently active language of the specified workflow.
  @Operation
  Language getLanguage(GetLanguageInput input);

  // Changes the active language on the specified workflow, returning the previous one.
  @Operation
  Language setLanguage(SetLanguageInput input);

  // Approves the specified workflow, allowing it to complete.
  @Operation
  ApproveOutput approve(ApproveInput input);
}
