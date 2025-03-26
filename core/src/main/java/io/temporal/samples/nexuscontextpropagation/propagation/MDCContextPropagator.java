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

package io.temporal.samples.nexuscontextpropagation.propagation;

import io.temporal.api.common.v1.Payload;
import io.temporal.common.context.ContextPropagator;
import io.temporal.common.converter.DataConverter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;

public class MDCContextPropagator implements ContextPropagator {

  @Override
  public String getName() {
    return this.getClass().getName();
  }

  @Override
  public Object getCurrentContext() {
    Map<String, String> context = new HashMap<>();
    if (MDC.getCopyOfContextMap() == null) {
      return context;
    }
    for (Map.Entry<String, String> entry : MDC.getCopyOfContextMap().entrySet()) {
      if (entry.getKey().startsWith("x-nexus-")) {
        context.put(entry.getKey(), entry.getValue());
      }
    }
    return context;
  }

  @Override
  public void setCurrentContext(Object context) {
    Map<String, String> contextMap = (Map<String, String>) context;
    for (Map.Entry<String, String> entry : contextMap.entrySet()) {
      MDC.put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public Map<String, Payload> serializeContext(Object context) {
    Map<String, String> contextMap = (Map<String, String>) context;
    Map<String, Payload> serializedContext = new HashMap<>();
    for (Map.Entry<String, String> entry : contextMap.entrySet()) {
      serializedContext.put(
          entry.getKey(), DataConverter.getDefaultInstance().toPayload(entry.getValue()).get());
    }
    return serializedContext;
  }

  @Override
  public Object deserializeContext(Map<String, Payload> context) {
    Map<String, String> contextMap = new HashMap<>();
    for (Map.Entry<String, Payload> entry : context.entrySet()) {
      contextMap.put(
          entry.getKey(),
          DataConverter.getDefaultInstance()
              .fromPayload(entry.getValue(), String.class, String.class));
    }
    return contextMap;
  }
}
