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

package io.temporal.samples.springboot.kafka;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class MessageController {
  private final List<SseEmitter> emitters = new ArrayList<>();

  @GetMapping("/kafka-messages")
  public SseEmitter getKafkaMessages() {

    SseEmitter emitter = new SseEmitter(60 * 1000L);
    emitters.add(emitter);

    emitter.onCompletion(() -> emitters.remove(emitter));

    emitter.onTimeout(() -> emitters.remove(emitter));

    return emitter;
  }

  public List<SseEmitter> getEmitters() {
    return emitters;
  }

  public SseEmitter getLatestEmitter() {
    return emitters.isEmpty() ? null : emitters.get(emitters.size() - 1);
  }
}
