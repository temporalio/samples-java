package io.temporal.samples.payloadconverter.cloudevents;

import com.google.protobuf.ByteString;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.format.EventSerializationException;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.DataConverterException;
import io.temporal.common.converter.PayloadConverter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Payload converter specific to CloudEvents format */
public class CloudEventsPayloadConverter implements PayloadConverter {

  private EventFormat CEFormat =
      EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);

  @Override
  public String getEncodingType() {
    return "json/plain";
  }

  @Override
  public Optional<Payload> toData(Object value) throws DataConverterException {

    try {
      CloudEvent cloudEvent = (CloudEvent) value;
      byte[] serialized = CEFormat.serialize(cloudEvent);

      return Optional.of(
          Payload.newBuilder()
              .putMetadata(
                  "encoding", ByteString.copyFrom(getEncodingType(), StandardCharsets.UTF_8))
              .setData(ByteString.copyFrom(serialized))
              .build());

    } catch (EventSerializationException | ClassCastException e) {
      throw new DataConverterException(e);
    }
  }

  @Override
  public <T> T fromData(Payload content, Class<T> valueClass, Type valueType)
      throws DataConverterException {
    try {
      return (T) CEFormat.deserialize(content.getData().toByteArray());
    } catch (ClassCastException e) {
      throw new DataConverterException(e);
    }
  }
}
