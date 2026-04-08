package io.temporal.samples.nexusmessaging.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;

@Service
public interface SampleNexusService {

  class SignalWorkflowInput {
    private final String name;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SignalWorkflowInput(@JsonProperty("name") String name) {
      this.name = name;
    }

    @JsonProperty("name")
    public String getName() {
      return name;
    }
  }

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  class SignalWorkflowOutput {
    @JsonCreator
    public SignalWorkflowOutput() {}
  }

  class QueryWorkflowInput {
    private final String name;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public QueryWorkflowInput(@JsonProperty("name") String name) {
      this.name = name;
    }

    @JsonProperty("name")
    public String getName() {
      return name;
    }
  }

  class QueryWorkflowRemoteStartInput {
    private final String name;
    private final String workflowId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public QueryWorkflowRemoteStartInput(
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

  class QueryWorkflowOutput {
    private final String message;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public QueryWorkflowOutput(@JsonProperty("message") String message) {
      this.message = message;
    }

    @JsonProperty("message")
    public String getMessage() {
      return message;
    }
  }

  class UpdateWorkflowInput {
    private final String name;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public UpdateWorkflowInput(@JsonProperty("name") String name) {
      this.name = name;
    }

    @JsonProperty("name")
    public String getName() {
      return name;
    }
  }

  class UpdateWorkflowOutput {
    private final int result;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public UpdateWorkflowOutput(@JsonProperty("result") int result) {
      this.result = result;
    }

    @JsonProperty("result")
    public int getResult() {
      return result;
    }
  }

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

  class RunFromRemoteOutput {
    private final String message;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RunFromRemoteOutput(@JsonProperty("message") String message) {
      this.message = message;
    }

    @JsonProperty("message")
    public String getMessage() {
      return message;
    }
  }

  @Operation
  RunFromRemoteOutput runFromRemote(RunFromRemoteInput input);

  class SignalWorkflowRemoteStartInput {
    private final String name;
    private final String workflowId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SignalWorkflowRemoteStartInput(
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

  @Operation
  SignalWorkflowOutput signalWorkflow(SignalWorkflowInput input);

  @Operation
  SignalWorkflowOutput signalWorkflowRemoteStart(SignalWorkflowRemoteStartInput input);

  @Operation
  UpdateWorkflowOutput updateWorkflow(UpdateWorkflowInput input);

  @Operation
  QueryWorkflowOutput queryWorkflow(QueryWorkflowInput input);

  @Operation
  QueryWorkflowOutput queryWorkflowRemoteStart(QueryWorkflowRemoteStartInput input);
}
