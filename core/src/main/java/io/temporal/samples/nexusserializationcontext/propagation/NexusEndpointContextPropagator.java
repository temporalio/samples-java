package io.temporal.samples.nexusserializationcontext.propagation;

import io.temporal.api.common.v1.Payload;
import io.temporal.common.context.ContextPropagator;
import io.temporal.common.converter.DataConverter;
import io.temporal.samples.nexusserializationcontext.SampleConfig;
import java.util.Map;
import org.slf4j.MDC;

/** Carries the Nexus endpoint from a handler into the workflow it starts. */
public final class NexusEndpointContextPropagator implements ContextPropagator {
  private static final DataConverter CONVERTER = DataConverter.getDefaultInstance();

  public static String currentEndpoint() {
    return MDC.get(SampleConfig.ENDPOINT_METADATA_KEY);
  }

  @Override
  public String getName() {
    return NexusEndpointContextPropagator.class.getName();
  }

  @Override
  public Object getCurrentContext() {
    return currentEndpoint();
  }

  @Override
  public void setCurrentContext(Object context) {
    if (context == null) {
      MDC.remove(SampleConfig.ENDPOINT_METADATA_KEY);
    } else {
      MDC.put(SampleConfig.ENDPOINT_METADATA_KEY, (String) context);
    }
  }

  @Override
  public Map<String, Payload> serializeContext(Object context) {
    if (context == null) {
      return Map.of();
    }
    return Map.of(
        SampleConfig.ENDPOINT_METADATA_KEY, CONVERTER.toPayload((String) context).orElseThrow());
  }

  @Override
  public Object deserializeContext(Map<String, Payload> header) {
    Payload payload = header.get(SampleConfig.ENDPOINT_METADATA_KEY);
    return payload == null ? null : CONVERTER.fromPayload(payload, String.class, String.class);
  }
}
