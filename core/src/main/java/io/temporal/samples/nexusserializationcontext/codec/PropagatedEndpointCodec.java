package io.temporal.samples.nexusserializationcontext.codec;

import io.temporal.api.common.v1.Payload;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.payload.codec.PayloadCodecException;
import io.temporal.payload.context.NexusSerializationContext;
import io.temporal.payload.context.SerializationContext;
import io.temporal.samples.nexusserializationcontext.propagation.NexusEndpointContextPropagator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;

/**
 * Selects a codec on the shared handler for workflow-backed asynchronous operations. The incoming
 * Nexus request has {@link NexusSerializationContext}; the backing workflow's payloads and final
 * result use the endpoint carried by the context propagator instead.
 */
public final class PropagatedEndpointCodec implements PayloadCodec {
  private final Map<String, PayloadCodec> codecsByEndpoint;

  public PropagatedEndpointCodec(Map<String, PayloadCodec> codecsByEndpoint) {
    this.codecsByEndpoint = Map.copyOf(codecsByEndpoint);
  }

  @Override
  @Nonnull
  public PayloadCodec withContext(@Nonnull SerializationContext context) {
    // The incoming Nexus request has this context; the backing workflow's result does not.
    if (context instanceof NexusSerializationContext nexusContext) {
      return codecFor(nexusContext.getEndpoint()).withContext(context);
    }
    return this;
  }

  @Override
  @Nonnull
  public List<Payload> encode(@Nonnull List<Payload> payloads) {
    return codecForCurrentEndpoint().encode(payloads);
  }

  @Override
  @Nonnull
  public List<Payload> decode(@Nonnull List<Payload> payloads) {
    return codecForCurrentEndpoint().decode(payloads);
  }

  private PayloadCodec codecForCurrentEndpoint() {
    // For backing workflow payloads, choose the codec from the propagated endpoint.
    String endpoint = NexusEndpointContextPropagator.currentEndpoint();
    if (StringUtils.isBlank(endpoint)) {
      throw new PayloadCodecException("Missing propagated Nexus endpoint");
    }
    return codecFor(endpoint);
  }

  private PayloadCodec codecFor(String endpoint) {
    PayloadCodec codec = codecsByEndpoint.get(endpoint);
    if (codec == null) {
      throw new PayloadCodecException("Unknown Nexus endpoint: " + endpoint);
    }
    return codec;
  }
}
