package io.temporal.samples.nexusserializationcontext;

import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.payload.codec.ChainCodec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class SampleConfig {
  public static final String COMPRESSED_ENCRYPTED_ENDPOINT =
      "nexus-serialization-compressed-encrypted";
  public static final String ENCRYPTED_ENDPOINT = "nexus-serialization-encrypted";
  public static final String HANDLER_TASK_QUEUE = "nexus-serialization-handler";
  public static final String CALLER_TASK_QUEUE = "nexus-serialization-caller";
  static final String KEY_A_ID = "key-a";
  static final String KEY_B_ID = "key-b";

  // Hard-coded keys are only for this local sample.
  private static final SecretKey KEY_A =
      new SecretKeySpec("sample-key-A-123".getBytes(StandardCharsets.UTF_8), "AES");
  private static final SecretKey KEY_B =
      new SecretKeySpec("sample-key-B-123".getBytes(StandardCharsets.UTF_8), "AES");

  private SampleConfig() {}

  public static DataConverter dataConverter() {
    return new CodecDataConverter(
        DefaultDataConverter.newDefaultInstance(),
        List.of(
            new NexusCodec(
                Map.of(
                    COMPRESSED_ENCRYPTED_ENDPOINT,
                    // ChainCodec encodes last to first: compress, then encrypt.
                    new ChainCodec(List.of(new AesGcmCodec(KEY_A_ID, KEY_A), new ZlibCodec())),
                    ENCRYPTED_ENDPOINT,
                    new AesGcmCodec(KEY_B_ID, KEY_B)))));
  }
}
