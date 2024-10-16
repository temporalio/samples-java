package io.temporal.samples.nexus.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;

// @@@SNIPSTART samples-java-nexus-service
@Service
public interface NexusService {
  enum Language {
    EN,
    FR,
    DE,
    ES,
    TR
  }

  class HelloInput {
    private final String name;
    private final Language language;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HelloInput(
        @JsonProperty("name") String name, @JsonProperty("language") Language language) {
      this.name = name;
      this.language = language;
    }

    @JsonProperty("name")
    public String getName() {
      return name;
    }

    @JsonProperty("language")
    public Language getLanguage() {
      return language;
    }
  }

  class HelloOutput {
    private final String message;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HelloOutput(@JsonProperty("message") String message) {
      this.message = message;
    }

    @JsonProperty("message")
    public String getMessage() {
      return message;
    }
  }

  class EchoInput {
    private final String message;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public EchoInput(@JsonProperty("message") String message) {
      this.message = message;
    }

    @JsonProperty("message")
    public String getMessage() {
      return message;
    }
  }

  class EchoOutput {
    private final String message;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public EchoOutput(@JsonProperty("message") String message) {
      this.message = message;
    }

    @JsonProperty("message")
    public String getMessage() {
      return message;
    }
  }

  @Operation
  HelloOutput hello(HelloInput input);

  @Operation
  EchoOutput echo(EchoInput input);
}
// @@@SNIPEND
