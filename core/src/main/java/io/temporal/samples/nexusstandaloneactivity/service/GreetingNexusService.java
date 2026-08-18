package io.temporal.samples.nexusstandaloneactivity.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;

// Nexus service definition shared by the caller and the handler. It declares a single operation
// whose backing execution is a standalone Activity.
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

  // Asynchronous operation: starting it starts a standalone Activity, and the operation completes
  // when that Activity returns its result.
  @Operation
  GreetingOutput greet(GreetingInput input);
}
