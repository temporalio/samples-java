package io.temporal.samples.nexusserializationcontext;

public final class SampleConfig {
  public static final String COMPRESSED_ENCRYPTED_1_ENDPOINT =
      "nexus-serialization-compressed-encrypted-1";
  public static final String ENCRYPTED_1_ENDPOINT = "nexus-serialization-encrypted-1";
  public static final String COMPRESSED_ENCRYPTED_2_ENDPOINT =
      "nexus-serialization-compressed-encrypted-2";
  public static final String ENCRYPTED_2_ENDPOINT = "nexus-serialization-encrypted-2";
  public static final String ENDPOINT_METADATA_KEY = "nexus-endpoint-name";
  public static final String COMPRESSED_ENCRYPTED_KEY_1_ID = "compressed-encrypted-1";
  public static final String ENCRYPTED_KEY_1_ID = "encrypted-1";
  public static final String COMPRESSED_ENCRYPTED_KEY_2_ID = "compressed-encrypted-2";
  public static final String ENCRYPTED_KEY_2_ID = "encrypted-2";
  public static final String CALLER_WORKFLOW_KEY_ID = "caller-workflow";

  // Hard-coded keys are only for this local sample.
  public static final String COMPRESSED_ENCRYPTED_KEY_1 = "handler-a-key-01";
  public static final String ENCRYPTED_KEY_1 = "handler-b-key-01";
  public static final String COMPRESSED_ENCRYPTED_KEY_2 = "handler-a-key-02";
  public static final String ENCRYPTED_KEY_2 = "handler-b-key-02";
  public static final String CALLER_WORKFLOW_KEY = "sample-key-C-123";

  private SampleConfig() {}
}
