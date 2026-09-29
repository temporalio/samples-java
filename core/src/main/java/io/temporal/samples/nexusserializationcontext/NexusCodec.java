package io.temporal.samples.nexusserializationcontext;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.payload.codec.PayloadCodecException;
import io.temporal.payload.context.NexusSerializationContext;
import io.temporal.payload.context.SerializationContext;
import io.temporal.samples.nexusserializationcontext.service.EchoService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import javax.annotation.Nonnull;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/** Selects a payload encoding from the Nexus endpoint, service, and operation. */
public final class NexusCodec implements PayloadCodec {
  private static final SecretKey HMAC_KEY =
      new SecretKeySpec(
          "nexus-serialization-sample-key".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  private static final int HMAC_LENGTH = 32;

  private final NexusEncoding selectedEncoding;

  public NexusCodec() {
    this.selectedEncoding = NexusEncoding.NONE;
  }

  private NexusCodec(NexusEncoding selectedEncoding) {
    this.selectedEncoding = selectedEncoding;
  }

  @Override
  @Nonnull
  public PayloadCodec withContext(@Nonnull SerializationContext context) {
    if (!(context instanceof NexusSerializationContext)) {
      return new NexusCodec();
    }
    NexusSerializationContext nexusSerializationContext = (NexusSerializationContext) context;
    if (!EchoService.SERVICE_NAME.equals(nexusSerializationContext.getService())
        || !EchoService.ECHO_OPERATION_NAME.equals(nexusSerializationContext.getOperation())) {
      throw new PayloadCodecException(
          "Unexpected Nexus service or operation: " + nexusSerializationContext);
    }
    if (SampleConfig.HMAC_ENDPOINT.equals(nexusSerializationContext.getEndpoint())) {
      return new NexusCodec(NexusEncoding.HMAC);
    }
    if (SampleConfig.ZLIB_ENDPOINT.equals(nexusSerializationContext.getEndpoint())) {
      return new NexusCodec(NexusEncoding.ZLIB);
    }
    throw new PayloadCodecException(
        "Unknown Nexus endpoint: " + nexusSerializationContext.getEndpoint());
  }

  @Override
  @Nonnull
  public List<Payload> encode(@Nonnull List<Payload> payloads) {
    return switch (selectedEncoding) {
      case HMAC -> encodeHmac(payloads);
      case ZLIB -> encodeZlib(payloads);
      case NONE -> payloads;
    };
  }

  @Override
  @Nonnull
  public List<Payload> decode(@Nonnull List<Payload> payloads) {
    return switch (selectedEncoding) {
      case HMAC -> decodeHmac(payloads);
      case ZLIB -> decodeZlib(payloads);
      case NONE -> payloads;
    };
  }

  private static List<Payload> encodeHmac(List<Payload> payloads) {
    List<Payload> encoded = new ArrayList<>(payloads.size());
    for (Payload payload : payloads) {
      byte[] bytes = payload.toByteArray();
      byte[] signature = hmac(bytes);
      byte[] signed =
          ByteBuffer.allocate(signature.length + bytes.length).put(signature).put(bytes).array();
      encoded.add(
          Payload.newBuilder()
              .putMetadata(
                  EncodingKeys.METADATA_ENCODING_KEY,
                  ByteString.copyFromUtf8(NexusEncoding.HMAC.encodingName()))
              .setData(ByteString.copyFrom(signed))
              .build());
    }
    return encoded;
  }

  private static List<Payload> decodeHmac(List<Payload> payloads) {
    List<Payload> decoded = new ArrayList<>(payloads.size());
    for (Payload payload : payloads) {
      String encoding =
          payload
              .getMetadataOrDefault(EncodingKeys.METADATA_ENCODING_KEY, ByteString.EMPTY)
              .toStringUtf8();
      if (!NexusEncoding.HMAC.encodingName().equals(encoding)) {
        throw new PayloadCodecException("Expected a Nexus HMAC payload");
      }
      byte[] signed = payload.getData().toByteArray();
      if (signed.length < HMAC_LENGTH) {
        throw new PayloadCodecException("Nexus HMAC payload is too short");
      }
      byte[] signature = Arrays.copyOfRange(signed, 0, HMAC_LENGTH);
      byte[] bytes = Arrays.copyOfRange(signed, HMAC_LENGTH, signed.length);
      if (!MessageDigest.isEqual(signature, hmac(bytes))) {
        throw new PayloadCodecException("Nexus HMAC does not match");
      }
      try {
        decoded.add(Payload.parseFrom(bytes));
      } catch (InvalidProtocolBufferException e) {
        throw new PayloadCodecException(e);
      }
    }
    return decoded;
  }

  private static List<Payload> encodeZlib(List<Payload> payloads) {
    List<Payload> encoded = new ArrayList<>(payloads.size());
    for (Payload payload : payloads) {
      encoded.add(
          Payload.newBuilder()
              .putMetadata(
                  EncodingKeys.METADATA_ENCODING_KEY,
                  ByteString.copyFromUtf8(NexusEncoding.ZLIB.encodingName()))
              .setData(ByteString.copyFrom(compress(payload.toByteArray())))
              .build());
    }
    return encoded;
  }

  private static List<Payload> decodeZlib(List<Payload> payloads) {
    List<Payload> decoded = new ArrayList<>(payloads.size());
    for (Payload payload : payloads) {
      String encoding =
          payload
              .getMetadataOrDefault(EncodingKeys.METADATA_ENCODING_KEY, ByteString.EMPTY)
              .toStringUtf8();
      if (!NexusEncoding.ZLIB.encodingName().equals(encoding)) {
        throw new PayloadCodecException("Expected a Nexus zlib payload");
      }
      try {
        decoded.add(Payload.parseFrom(decompress(payload.getData().toByteArray())));
      } catch (InvalidProtocolBufferException e) {
        throw new PayloadCodecException(e);
      }
    }
    return decoded;
  }

  private static byte[] hmac(byte[] bytes) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(HMAC_KEY);
      return mac.doFinal(bytes);
    } catch (GeneralSecurityException e) {
      throw new PayloadCodecException(e);
    }
  }

  private static byte[] compress(byte[] bytes) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      try (DeflaterOutputStream deflater = new DeflaterOutputStream(output)) {
        deflater.write(bytes);
      }
      return output.toByteArray();
    } catch (IOException e) {
      throw new PayloadCodecException(e);
    }
  }

  private static byte[] decompress(byte[] bytes) {
    try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(bytes))) {
      return inflater.readAllBytes();
    } catch (IOException e) {
      throw new PayloadCodecException(e);
    }
  }
}
