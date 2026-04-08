package io.temporal.samples.nexus_sync_operations.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;
import java.util.List;

/**
 * Nexus service definition. Shared between the handler and caller. The caller uses this to create a
 * type-safe Nexus client stub; the handler implements the operations.
 */
@Service
public interface NexusGreetingService {

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
  class GetLanguageInput {
    @JsonCreator
    public GetLanguageInput() {}
  }

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

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  class ApproveOutput {
    @JsonCreator
    public ApproveOutput() {}
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

  // Returns the languages supported by the greeting workflow.
  @Operation
  GetLanguagesOutput getLanguages(GetLanguagesInput input);

  // Returns the currently active language.
  @Operation
  Language getLanguage(GetLanguageInput input);

  // Changes the active language, returning the previous one.
  @Operation
  Language setLanguage(SetLanguageInput input);

  // Approves the workflow, allowing it to complete.
  @Operation
  ApproveOutput approve(ApproveInput input);
}
