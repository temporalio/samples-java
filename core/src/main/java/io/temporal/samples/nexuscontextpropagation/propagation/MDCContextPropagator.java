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
