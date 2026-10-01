package io.temporal.samples.nexusserializationcontext.codec;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.payload.codec.PayloadCodecException;
import io.temporal.payload.context.NexusSerializationContext;
import io.temporal.payload.context.SerializationContext;
import io.temporal.samples.nexusserializationcontext.SampleConfig;
import io.temporal.samples.nexusserializationcontext.propagation.NexusEndpointContextPropagator;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.apache.commons.lang.StringUtils;

/** Encrypts Nexus payloads with the key assigned to an endpoint. */
public final class AesGcmCodec implements PayloadCodec {
  static final String KEY_ID_METADATA_KEY = "encryption-key-id";

  private static final String CIPHER = "AES/GCM/NoPadding";
  private static final int NONCE_LENGTH = 12;
  private static final int TAG_LENGTH_BITS = 128;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final String keyId;
  private final SecretKey key;
  private final String endpoint;

  public AesGcmCodec(String keyId, SecretKey key) {
    this.keyId = keyId;
    this.key = key;
    this.endpoint = null;
  }

  private AesGcmCodec(String keyId, SecretKey key, String endpoint) {
    this.keyId = keyId;
    this.key = key;
    this.endpoint = endpoint;
  }

  @Override
  @Nonnull
  public PayloadCodec withContext(@Nonnull SerializationContext context) {
    if (context instanceof NexusSerializationContext nexusSerializationContext) {
      return new AesGcmCodec(keyId, key, nexusSerializationContext.getEndpoint());
    }
    return this;
  }

  @Override
  @Nonnull
  public List<Payload> encode(@Nonnull List<Payload> payloads) {
    String endpointName =
        StringUtils.defaultIfBlank(endpoint, NexusEndpointContextPropagator.currentEndpoint());
    List<Payload> encoded = new ArrayList<>(payloads.size());
    for (Payload payload : payloads) {
      Payload.Builder encrypted =
          Payload.newBuilder()
              .putMetadata(
                  EncodingKeys.METADATA_ENCODING_KEY,
                  ByteString.copyFromUtf8(NexusEncoding.AES_GCM.encodingName()))
              .putMetadata(KEY_ID_METADATA_KEY, ByteString.copyFromUtf8(keyId))
              .setData(ByteString.copyFrom(encrypt(payload.toByteArray())));
      if (StringUtils.isNotBlank(endpointName)) {
        encrypted.putMetadata(
            SampleConfig.ENDPOINT_METADATA_KEY, ByteString.copyFromUtf8(endpointName));
      }
      encoded.add(encrypted.build());
    }
    return encoded;
  }

  @Override
  @Nonnull
  public List<Payload> decode(@Nonnull List<Payload> payloads) {
    List<Payload> decoded = new ArrayList<>(payloads.size());
    for (Payload payload : payloads) {
      String encoding =
          payload
              .getMetadataOrDefault(EncodingKeys.METADATA_ENCODING_KEY, ByteString.EMPTY)
              .toStringUtf8();
      if (!NexusEncoding.AES_GCM.encodingName().equals(encoding)) {
        throw new PayloadCodecException("Expected a Nexus AES-GCM payload");
      }
      String payloadKeyId =
          payload.getMetadataOrDefault(KEY_ID_METADATA_KEY, ByteString.EMPTY).toStringUtf8();
      if (!keyId.equals(payloadKeyId)) {
        throw new PayloadCodecException("Unexpected encryption key ID: " + payloadKeyId);
      }
      try {
        decoded.add(Payload.parseFrom(decrypt(payload.getData().toByteArray())));
      } catch (InvalidProtocolBufferException e) {
        throw new PayloadCodecException(e);
      }
    }
    return decoded;
  }

  private byte[] encrypt(byte[] bytes) {
    byte[] nonce = new byte[NONCE_LENGTH];
    RANDOM.nextBytes(nonce);
    try {
      Cipher cipher = Cipher.getInstance(CIPHER);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
      byte[] ciphertext = cipher.doFinal(bytes);
      return ByteBuffer.allocate(nonce.length + ciphertext.length)
          .put(nonce)
          .put(ciphertext)
          .array();
    } catch (GeneralSecurityException e) {
      throw new PayloadCodecException(e);
    }
  }

  private byte[] decrypt(byte[] encrypted) {
    if (encrypted.length < NONCE_LENGTH + TAG_LENGTH_BITS / Byte.SIZE) {
      throw new PayloadCodecException("Nexus AES-GCM payload is too short");
    }
    ByteBuffer buffer = ByteBuffer.wrap(encrypted);
    byte[] nonce = new byte[NONCE_LENGTH];
    buffer.get(nonce);
    byte[] ciphertext = new byte[buffer.remaining()];
    buffer.get(ciphertext);
    try {
      Cipher cipher = Cipher.getInstance(CIPHER);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
      return cipher.doFinal(ciphertext);
    } catch (GeneralSecurityException e) {
      throw new PayloadCodecException(e);
    }
  }
}
