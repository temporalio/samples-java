package io.temporal.samples.nexusserializationcontext.codec;

import io.temporal.api.common.v1.Payload;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.payload.codec.PayloadCodecException;
import io.temporal.payload.context.NexusSerializationContext;
import io.temporal.payload.context.SerializationContext;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Selects an endpoint codec for synchronous and asynchronous Nexus payloads. The caller's own
 * workflow payloads use a separate codec.
 */
public final class NexusCodec implements PayloadCodec {
  private final Map<String, PayloadCodec> codecsByEndpoint;
  // The caller workflow has no Nexus endpoint, so Key C encrypts its input and final result.
  private final PayloadCodec workflowCodec;

  public NexusCodec(Map<String, PayloadCodec> codecsByEndpoint, PayloadCodec workflowCodec) {
    this.codecsByEndpoint = Map.copyOf(codecsByEndpoint);
    this.workflowCodec = workflowCodec;
  }

  @Override
  @Nonnull
  public PayloadCodec withContext(@Nonnull SerializationContext context) {
    if (context instanceof NexusSerializationContext nexusContext) {
      return codecFor(nexusContext.getEndpoint()).withContext(context);
    }
    // Caller workflow input and result have no Nexus endpoint; use the caller's Key C codec.
    return workflowCodec.withContext(context);
  }

  @Override
  @Nonnull
  public List<Payload> encode(@Nonnull List<Payload> payloads) {
    return workflowCodec.encode(payloads);
  }

  @Override
  @Nonnull
  public List<Payload> decode(@Nonnull List<Payload> payloads) {
    return workflowCodec.decode(payloads);
  }

  private PayloadCodec codecFor(String endpoint) {
    PayloadCodec codec = codecsByEndpoint.get(endpoint);
    if (codec == null) {
      throw new PayloadCodecException("Unknown Nexus endpoint: " + endpoint);
    }
    return codec;
  }
}
