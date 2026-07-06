package io.temporal.samples.nexusstandalone.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;

// Shared Nexus service definition for the standalone-Nexus sample. It declares two operations:
//   - startGreeting: backed by a workflow that blocks (long-running), so the client can demonstrate
//     cancel/terminate against an operation that is still running.
//   - greet: synchronous, completes immediately, so the client can demonstrate execute.
@Service
public interface GreetingNexusService {

  class GreetingInput {
    private final String name;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GreetingInput(@JsonProperty("name") String name) {
      this.name = name;
    }

    @JsonProperty("name")
    public String getName() {
      return name;
    }
  }

  class GreetingOutput {
    private final String message;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GreetingOutput(@JsonProperty("message") String message) {
      this.message = message;
    }

    @JsonProperty("message")
    public String getMessage() {
      return message;
    }
  }

  // An asynchronous operation backed by a workflow that blocks indefinitely, so the operation stays
  // running until the caller cancels or terminates it.
  @Operation
  GreetingOutput startGreeting(GreetingInput input);

  // A synchronous operation that completes immediately. Used to demonstrate execute, which blocks
  // on the operation result.
  @Operation
  GreetingOutput greet(GreetingInput input);
}
