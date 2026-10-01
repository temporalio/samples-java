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
 * Selects a codec when the SDK provides {@link NexusSerializationContext}. The caller uses this for
 * both synchronous and asynchronous Nexus requests and results. Non-Nexus workflow payloads pass
 * through unchanged.
 */
public final class NexusCodec implements PayloadCodec {
  private final Map<String, PayloadCodec> codecsByEndpoint;

  public NexusCodec(Map<String, PayloadCodec> codecsByEndpoint) {
    this.codecsByEndpoint = Map.copyOf(codecsByEndpoint);
  }

  @Override
  @Nonnull
  public PayloadCodec withContext(@Nonnull SerializationContext context) {
    if (!(context instanceof NexusSerializationContext)) {
      return this;
    }
    return codecFor(((NexusSerializationContext) context).getEndpoint()).withContext(context);
  }

  @Override
  @Nonnull
  public List<Payload> encode(@Nonnull List<Payload> payloads) {
    return payloads;
  }

  @Override
  @Nonnull
  public List<Payload> decode(@Nonnull List<Payload> payloads) {
    return payloads;
  }

  private PayloadCodec codecFor(String endpoint) {
    PayloadCodec codec = codecsByEndpoint.get(endpoint);
    if (codec == null) {
      throw new PayloadCodecException("Unknown Nexus endpoint: " + endpoint);
    }
    return codec;
  }
}
