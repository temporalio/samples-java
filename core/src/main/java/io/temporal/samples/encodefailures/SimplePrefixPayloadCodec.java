

package io.temporal.samples.encodefailures;

import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Payload;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.payload.codec.PayloadCodecException;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Simple codec that adds dummy prefix to payload. For this sample it's also applied for failure
 * messages.
 */
public class SimplePrefixPayloadCodec implements PayloadCodec {

  public static final ByteString PREFIX = ByteString.copyFromUtf8("Customer: ");

  @NotNull
  @Override
  public List<Payload> encode(@NotNull List<Payload> payloads) {
    return payloads.stream().map(this::encode).collect(Collectors.toList());
  }

  private Payload encode(Payload decodedPayload) {
    ByteString encodedData = PREFIX.concat(decodedPayload.getData());
    return decodedPayload.toBuilder().setData(encodedData).build();
  }

  @NotNull
  @Override
  public List<Payload> decode(@NotNull List<Payload> payloads) {
    return payloads.stream().map(this::decode).collect(Collectors.toList());
  }

  private Payload decode(Payload encodedPayload) {
    ByteString encodedData = encodedPayload.getData();
    if (!encodedData.startsWith(PREFIX))
      throw new PayloadCodecException("Payload is not correctly encoded");
    ByteString decodedData = encodedData.substring(PREFIX.size());
    return encodedPayload.toBuilder().setData(decodedData).build();
  }
}
