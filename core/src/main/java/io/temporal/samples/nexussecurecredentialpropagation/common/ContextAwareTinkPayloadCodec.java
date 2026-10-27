package io.temporal.samples.nexussecurecredentialpropagation.common;

import com.google.crypto.tink.Aead;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.failure.ApplicationFailure;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.payload.codec.PayloadCodecException;
import io.temporal.payload.context.HasWorkflowSerializationContext;
import io.temporal.payload.context.NexusSerializationContext;
import io.temporal.payload.context.SerializationContext;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Encrypts payloads with Tink AEAD, binding each ciphertext to the serialization context it was
 * produced for.
 *
 * <p>The context descriptor is stored in the payload metadata and authenticated as associated data,
 * so a ciphertext cannot be relabelled with a different context. Decoding requires the descriptor
 * to match the decoder's own context, with one exception: the result or failure of an asynchronous
 * Nexus Operation is produced by the handler Workflow under its Workflow context and decoded by the
 * caller under the Nexus context, so a Nexus decoder accepts Workflow-context payloads.
 *
 * <p>The codec also reports every encoded and decoded list to {@link ArgsDigest}. It must be the
 * outermost codec so that digests cover the bytes actually transmitted.
 */
public final class ContextAwareTinkPayloadCodec implements PayloadCodec {
  static final String ENCODING = "binary/tink-aead";
  static final String PROFILE_METADATA_KEY = "tink-profile";
  static final String CONTEXT_METADATA_KEY = "tink-context";
  static final String PROFILE_CONTEXT = "v1.context";
  static final String PROFILE_CONTEXT_FREE = "v1.context-free";

  /** Error type the SDK reports as a non-retryable Nexus {@code BAD_REQUEST}. */
  static final String PAYLOAD_VALIDATION_ERROR = "PayloadValidationError";

  private static final String NEXUS = "nexus";
  private static final String WORKFLOW = "workflow";
  private static final int MAX_CONTEXT_LENGTH = 4 * 1024;

  private final Aead aead;
  private final @Nullable byte[] context;

  public ContextAwareTinkPayloadCodec(Aead aead) {
    this(aead, null);
  }

  private ContextAwareTinkPayloadCodec(Aead aead, @Nullable byte[] context) {
    this.aead = aead;
    this.context = context;
  }

  @Override
  @Nonnull
  public PayloadCodec withContext(@Nonnull SerializationContext serializationContext) {
    if (serializationContext instanceof NexusSerializationContext) {
      NexusSerializationContext nexus = (NexusSerializationContext) serializationContext;
      return new ContextAwareTinkPayloadCodec(
          aead,
          Frame.of(NEXUS)
              .add(nexus.getEndpoint())
              .add(nexus.getService())
              .add(nexus.getOperation())
              .toByteArray());
    }
    if (serializationContext instanceof HasWorkflowSerializationContext) {
      HasWorkflowSerializationContext workflow =
          (HasWorkflowSerializationContext) serializationContext;
      return new ContextAwareTinkPayloadCodec(
          aead,
          Frame.of(WORKFLOW)
              .add(workflow.getNamespace())
              .add(workflow.getWorkflowId())
              .toByteArray());
    }
    return new ContextAwareTinkPayloadCodec(aead, null);
  }

  @Override
  @Nonnull
  public List<Payload> encode(@Nonnull List<Payload> payloads) {
    String profile = context == null ? PROFILE_CONTEXT_FREE : PROFILE_CONTEXT;
    byte[] descriptor = context == null ? new byte[0] : context;
    List<Payload> encoded = new ArrayList<>(payloads.size());
    for (Payload payload : payloads) {
      byte[] ciphertext;
      try {
        ciphertext = aead.encrypt(payload.toByteArray(), associatedData(profile, descriptor));
      } catch (GeneralSecurityException e) {
        throw new PayloadCodecException(e);
      }
      Payload.Builder builder =
          Payload.newBuilder()
              .putMetadata(EncodingKeys.METADATA_ENCODING_KEY, ByteString.copyFromUtf8(ENCODING))
              .putMetadata(PROFILE_METADATA_KEY, ByteString.copyFromUtf8(profile))
              .setData(ByteString.copyFrom(ciphertext));
      if (context != null) {
        builder.putMetadata(CONTEXT_METADATA_KEY, ByteString.copyFrom(context));
      }
      encoded.add(builder.build());
    }
    ArgsDigest.onEncoded(encoded);
    return encoded;
  }

  @Override
  @Nonnull
  public List<Payload> decode(@Nonnull List<Payload> payloads) {
    List<Payload> decoded = new ArrayList<>(payloads.size());
    for (Payload payload : payloads) {
      decoded.add(decodeOne(payload));
    }
    ArgsDigest.onDecoded(payloads);
    return decoded;
  }

  private Payload decodeOne(Payload payload) {
    String encoding =
        payload
            .getMetadataOrDefault(EncodingKeys.METADATA_ENCODING_KEY, ByteString.EMPTY)
            .toStringUtf8();
    if (!ENCODING.equals(encoding)) {
      throw rejected("Expected a Tink AEAD payload");
    }
    String profile =
        payload.getMetadataOrDefault(PROFILE_METADATA_KEY, ByteString.EMPTY).toStringUtf8();
    byte[] descriptor;
    if (PROFILE_CONTEXT.equals(profile)) {
      ByteString stored = payload.getMetadataOrDefault(CONTEXT_METADATA_KEY, null);
      if (stored == null || stored.isEmpty() || stored.size() > MAX_CONTEXT_LENGTH) {
        throw rejected("Missing or invalid payload context");
      }
      descriptor = stored.toByteArray();
      if (context != null
          && !Arrays.equals(context, descriptor)
          && !isAsyncNexusResult(descriptor)) {
        throw rejected("Payload context does not match");
      }
    } else if (PROFILE_CONTEXT_FREE.equals(profile)) {
      if (context != null) {
        // Never accept weaker associated data where a context is known.
        throw rejected("Context-free payload where a context is required");
      }
      descriptor = new byte[0];
    } else {
      throw rejected("Unknown payload profile");
    }
    try {
      byte[] plaintext =
          aead.decrypt(payload.getData().toByteArray(), associatedData(profile, descriptor));
      return Payload.parseFrom(plaintext);
    } catch (GeneralSecurityException | InvalidProtocolBufferException e) {
      throw rejected("Payload failed authentication");
    }
  }

  /**
   * A payload this codec will never accept. Thrown as a non-retryable {@code
   * PayloadValidationError} so that a Nexus handler reports it as a non-retryable {@code
   * BAD_REQUEST} instead of retrying it as a codec outage.
   */
  private static ApplicationFailure rejected(String reason) {
    return ApplicationFailure.newNonRetryableFailure(reason, PAYLOAD_VALIDATION_ERROR);
  }

  /**
   * A Nexus decoder accepts Workflow-context payloads: they are results and failures of
   * asynchronous operations, produced by the handler Workflow.
   */
  private boolean isAsyncNexusResult(byte[] descriptor) {
    return startsWithLabel(context, NEXUS) && startsWithLabel(descriptor, WORKFLOW);
  }

  private static boolean startsWithLabel(byte[] frame, String label) {
    try {
      return label.equals(new Frame.Reader(frame).nextString(label.length()));
    } catch (GeneralSecurityException e) {
      return false;
    }
  }

  private static byte[] associatedData(String profile, byte[] descriptor) {
    return Frame.of("payload").add(profile).add(descriptor).toByteArray();
  }
}
