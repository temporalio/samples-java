package io.temporal.samples.nexusserializationcontext;

import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import java.util.Collections;

public final class SampleConfig {
  public static final String HMAC_ENDPOINT = "nexus-serialization-hmac";
  public static final String ZLIB_ENDPOINT = "nexus-serialization-zlib";
  public static final String HMAC_HANDLER_TASK_QUEUE = "nexus-serialization-hmac-handler";
  public static final String ZLIB_HANDLER_TASK_QUEUE = "nexus-serialization-zlib-handler";
  public static final String CALLER_TASK_QUEUE = "nexus-serialization-caller";

  private SampleConfig() {}

  public static DataConverter dataConverter() {
    return new CodecDataConverter(
        DefaultDataConverter.newDefaultInstance(), Collections.singletonList(new NexusCodec()));
  }
}
