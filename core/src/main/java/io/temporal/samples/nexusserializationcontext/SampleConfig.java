package io.temporal.samples.nexusserializationcontext;

public final class SampleConfig {
  public static final String COMPRESSED_ENCRYPTED_ENDPOINT =
      "nexus-serialization-compressed-encrypted";
  public static final String ENCRYPTED_ENDPOINT = "nexus-serialization-encrypted";
  public static final String ASYNC_ENCRYPTED_ENDPOINT = "nexus-serialization-async-encrypted";
  public static final String ASYNC_COMPRESSED_ENCRYPTED_ENDPOINT =
      "nexus-serialization-async-compressed-encrypted";
  public static final String ENDPOINT_METADATA_KEY = "nexus-endpoint-name";
  public static final String KEY_A_ID = "key-a";
  public static final String KEY_B_ID = "key-b";
  public static final String KEY_C_ID = "key-c";
  public static final String KEY_D_ID = "key-d";

  // Hard-coded keys are only for this local sample.
  public static final String KEY_A_VALUE = "sample-key-A-123";
  public static final String KEY_B_VALUE = "sample-key-B-123";
  public static final String KEY_C_VALUE = "sample-key-C-123";
  public static final String KEY_D_VALUE = "sample-key-D-123";

  private SampleConfig() {}
}
