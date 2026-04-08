package io.temporal.samples.nexus_sync_operations.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;

/**
 * Nexus service definition for the remote-start pattern. Unlike {@link NexusGreetingService}, every
 * operation includes a {@code workflowId} so the caller controls which workflow instance is
 * targeted. This also exposes a {@code runFromRemote} operation that starts a new GreetingWorkflow.
 */
@Service
public interface NexusRemoteGreetingService {

  class RunFromRemoteInput {
    private final String workflowId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RunFromRemoteInput(@JsonProperty("workflowId") String workflowId) {
      this.workflowId = workflowId;
    }

    @JsonProperty("workflowId")
    public String getWorkflowId() {
      return workflowId;
    }
  }

  class GetLanguagesInput {
    private final boolean includeUnsupported;
    private final String workflowId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GetLanguagesInput(
        @JsonProperty("includeUnsupported") boolean includeUnsupported,
        @JsonProperty("workflowId") String workflowId) {
      this.includeUnsupported = includeUnsupported;
      this.workflowId = workflowId;
    }

    @JsonProperty("includeUnsupported")
    public boolean isIncludeUnsupported() {
      return includeUnsupported;
    }

    @JsonProperty("workflowId")
    public String getWorkflowId() {
      return workflowId;
    }
  }

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  class GetLanguageInput {
    private final String workflowId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GetLanguageInput(@JsonProperty("workflowId") String workflowId) {
      this.workflowId = workflowId;
    }

    @JsonProperty("workflowId")
    public String getWorkflowId() {
      return workflowId;
    }
  }

  class SetLanguageInput {
    private final Language language;
    private final String workflowId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SetLanguageInput(
        @JsonProperty("language") Language language,
        @JsonProperty("workflowId") String workflowId) {
      this.language = language;
      this.workflowId = workflowId;
    }

    @JsonProperty("language")
    public Language getLanguage() {
      return language;
    }

    @JsonProperty("workflowId")
    public String getWorkflowId() {
      return workflowId;
    }
  }

  class ApproveInput {
    private final String name;
    private final String workflowId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ApproveInput(
        @JsonProperty("name") String name, @JsonProperty("workflowId") String workflowId) {
      this.name = name;
      this.workflowId = workflowId;
    }

    @JsonProperty("name")
    public String getName() {
      return name;
    }

    @JsonProperty("workflowId")
    public String getWorkflowId() {
      return workflowId;
    }
  }

  // Starts a new GreetingWorkflow with the given workflow ID. This is an asynchronous Nexus
  // operation: the caller receives a handle and can wait for the workflow to complete.
  @Operation
  String runFromRemote(RunFromRemoteInput input);

  // Returns the languages supported by the specified workflow.
  @Operation
  NexusGreetingService.GetLanguagesOutput getLanguages(GetLanguagesInput input);

  // Returns the currently active language of the specified workflow.
  @Operation
  Language getLanguage(GetLanguageInput input);

  // Changes the active language on the specified workflow, returning the previous one.
  @Operation
  Language setLanguage(SetLanguageInput input);

  // Approves the specified workflow, allowing it to complete.
  @Operation
  NexusGreetingService.ApproveOutput approve(ApproveInput input);
}
