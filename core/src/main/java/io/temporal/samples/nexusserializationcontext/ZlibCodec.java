package io.temporal.samples.nexusserializationcontext;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.payload.codec.PayloadCodecException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import javax.annotation.Nonnull;

/** Compresses Nexus payloads with zlib. */
public final class ZlibCodec implements PayloadCodec {
  @Override
  @Nonnull
  public List<Payload> encode(@Nonnull List<Payload> payloads) {
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

  @Override
  @Nonnull
  public List<Payload> decode(@Nonnull List<Payload> payloads) {
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
