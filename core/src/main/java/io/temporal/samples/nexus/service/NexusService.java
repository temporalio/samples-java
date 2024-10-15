/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.nexus.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;

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
