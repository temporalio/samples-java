package io.temporal.samples.nexusserializationcontext.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.payload.codec.PayloadCodecException;
import io.temporal.payload.context.NexusSerializationContext;
import io.temporal.payload.context.WorkflowSerializationContext;
import io.temporal.samples.nexusserializationcontext.SampleConfig;
import io.temporal.samples.nexusserializationcontext.caller.CallerWorker;
import io.temporal.samples.nexusserializationcontext.caller.EndpointResults;
import io.temporal.samples.nexusserializationcontext.handler.CompressedEncryptedHandlerWorker;
import io.temporal.samples.nexusserializationcontext.handler.EncryptedHandlerWorker;
import io.temporal.samples.nexusserializationcontext.service.SyncEchoService;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class NexusCodecTest {
  @Test
  void callerAndHandlersUseTheKeyForEachEndpoint() throws Exception {
    DataConverter compressedEncryptedHandler = CompressedEncryptedHandlerWorker.dataConverter();
    DataConverter encryptedHandler = EncryptedHandlerWorker.dataConverter();

    assertEndpointRoundTrip(
        compressedEncryptedHandler,
        SampleConfig.COMPRESSED_ENCRYPTED_1_ENDPOINT,
        SampleConfig.COMPRESSED_ENCRYPTED_KEY_1_ID,
        "handler-a-key-01");
    assertEndpointRoundTrip(
        compressedEncryptedHandler,
        SampleConfig.COMPRESSED_ENCRYPTED_2_ENDPOINT,
        SampleConfig.COMPRESSED_ENCRYPTED_KEY_2_ID,
        "handler-a-key-02");
    assertEndpointRoundTrip(
        encryptedHandler,
        SampleConfig.ENCRYPTED_1_ENDPOINT,
        SampleConfig.ENCRYPTED_KEY_1_ID,
        "handler-b-key-01");
    assertEndpointRoundTrip(
        encryptedHandler,
        SampleConfig.ENCRYPTED_2_ENDPOINT,
        SampleConfig.ENCRYPTED_KEY_2_ID,
        "handler-b-key-02");
  }

  @Test
  void compressesBeforeEncryptingWithEitherKey() {
    assertCompressionReducesPayload(
        SampleConfig.COMPRESSED_ENCRYPTED_1_ENDPOINT, SampleConfig.ENCRYPTED_1_ENDPOINT);
    assertCompressionReducesPayload(
        SampleConfig.COMPRESSED_ENCRYPTED_2_ENDPOINT, SampleConfig.ENCRYPTED_2_ENDPOINT);
  }

  @Test
  void callerWorkflowPayloadsUseTheirOwnKey() throws Exception {
    DataConverter converter =
        CallerWorker.dataConverter()
            .withContext(
                new WorkflowSerializationContext("nexus-serialization-caller", "caller-id"));
    EndpointResults results =
        new EndpointResults(
            "reply compressed-encrypted-1",
            "reply encrypted-1",
            "reply compressed-encrypted-2",
            "reply encrypted-2",
            "async reply compressed-encrypted-1",
            "async reply encrypted-1",
            "async reply compressed-encrypted-2",
            "async reply encrypted-2");
    Payload input = converter.toPayload("Hello from Nexus").orElseThrow();
    Payload result = converter.toPayload(results).orElseThrow();

    assertEquals(
        NexusEncoding.AES_GCM.encodingName(),
        input.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.CALLER_WORKFLOW_KEY_ID,
        input.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.CALLER_WORKFLOW_KEY_ID,
        result.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertFalse(input.containsMetadata(SampleConfig.ENDPOINT_METADATA_KEY));
    assertFalse(result.containsMetadata(SampleConfig.ENDPOINT_METADATA_KEY));
    assertFalse(input.getData().toStringUtf8().contains("Hello from Nexus"));
    assertEncryptedWithKey(input, "sample-key-C-123");
    assertEncryptedWithKey(result, "sample-key-C-123");
    assertEquals("Hello from Nexus", converter.fromPayload(input, String.class, String.class));
    assertEquals(
        results, converter.fromPayload(result, EndpointResults.class, EndpointResults.class));
  }

  @Test
  void rejectsUnknownEndpoints() {
    DataConverter converter = converterFor("unknown");
    assertThrows(PayloadCodecException.class, () -> converter.toPayload("hello"));
  }

  @Test
  void handlerWorkflowInputsUseTheirDefaultKeys() throws Exception {
    WorkflowSerializationContext context =
        new WorkflowSerializationContext("handler", "workflow-id");
    assertWorkflowDefault(
        CompressedEncryptedHandlerWorker.dataConverter().withContext(context),
        SampleConfig.COMPRESSED_ENCRYPTED_KEY_1_ID,
        "handler-a-key-01");
    assertWorkflowDefault(
        EncryptedHandlerWorker.dataConverter().withContext(context),
        SampleConfig.ENCRYPTED_KEY_1_ID,
        "handler-b-key-01");
  }

  @Test
  void bindingAnotherEndpointKeepsEarlierBindings() throws Exception {
    DataConverter base = CallerWorker.dataConverter();
    DataConverter first = base.withContext(contextFor(SampleConfig.ENCRYPTED_1_ENDPOINT));
    DataConverter second = base.withContext(contextFor(SampleConfig.ENCRYPTED_2_ENDPOINT));
    DataConverter workflow =
        first.withContext(new WorkflowSerializationContext("caller", "caller-id"));

    Payload firstPayload = first.toPayload("hello").orElseThrow();
    assertEncryptionMetadata(
        firstPayload, SampleConfig.ENCRYPTED_1_ENDPOINT, SampleConfig.ENCRYPTED_KEY_1_ID);
    assertEncryptedWithKey(firstPayload, "handler-b-key-01");
    assertEquals("hello", first.fromPayload(firstPayload, String.class, String.class));

    Payload secondPayload = second.toPayload("reply").orElseThrow();
    assertEncryptionMetadata(
        secondPayload, SampleConfig.ENCRYPTED_2_ENDPOINT, SampleConfig.ENCRYPTED_KEY_2_ID);
    assertEncryptedWithKey(secondPayload, "handler-b-key-02");
    assertEquals("reply", second.fromPayload(secondPayload, String.class, String.class));

    assertWorkflowDefault(base, SampleConfig.CALLER_WORKFLOW_KEY_ID, "sample-key-C-123");
    assertWorkflowDefault(workflow, SampleConfig.CALLER_WORKFLOW_KEY_ID, "sample-key-C-123");

    DataConverter handler = EncryptedHandlerWorker.dataConverter();
    handler.withContext(contextFor(SampleConfig.ENCRYPTED_1_ENDPOINT)).toPayload("hello");
    assertWorkflowDefault(
        handler.withContext(new WorkflowSerializationContext("handler", "workflow-id")),
        SampleConfig.ENCRYPTED_KEY_1_ID,
        "handler-b-key-01");
  }

  @Test
  void encryptingTheSameValueUsesAFreshNonce() {
    DataConverter converter = converterFor(SampleConfig.ENCRYPTED_1_ENDPOINT);
    Payload first = converter.toPayload("hello").orElseThrow();
    Payload second = converter.toPayload("hello").orElseThrow();
    assertNotEquals(first.getData().substring(0, 12), second.getData().substring(0, 12));
    assertNotEquals(first.getData(), second.getData());
    assertEquals("hello", converter.fromPayload(first, String.class, String.class));
    assertEquals("hello", converter.fromPayload(second, String.class, String.class));
  }

  @Test
  void rejectsAlteredAndTruncatedCiphertext() {
    DataConverter converter = converterFor(SampleConfig.ENCRYPTED_1_ENDPOINT);
    Payload original = converter.toPayload("hello").orElseThrow();
    byte[] bytes = original.getData().toByteArray();
    byte[] altered = bytes.clone();
    altered[12] ^= 1;
    for (byte[] damaged :
        List.of(altered, Arrays.copyOf(bytes, 1), Arrays.copyOf(bytes, bytes.length - 1))) {
      Payload payload = original.toBuilder().setData(ByteString.copyFrom(damaged)).build();
      assertThrows(
          PayloadCodecException.class,
          () -> converter.fromPayload(payload, String.class, String.class));
    }
  }

  private static void assertWorkflowDefault(DataConverter converter, String keyId, String key)
      throws Exception {
    Payload input = converter.toPayload("hello").orElseThrow();
    assertEquals(keyId, input.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertFalse(input.containsMetadata(SampleConfig.ENDPOINT_METADATA_KEY));
    assertEncryptedWithKey(input, key);
    assertEquals("hello", converter.fromPayload(input, String.class, String.class));
  }

  private static void assertEndpointRoundTrip(
      DataConverter handler, String endpoint, String keyId, String key) throws Exception {
    DataConverter caller = converterFor(endpoint);
    DataConverter contextualHandler = handler.withContext(contextFor(endpoint));
    Payload request = caller.toPayload("hello").orElseThrow();
    assertEncryptionMetadata(request, endpoint, keyId);
    assertEncryptedWithKey(request, key);
    assertEquals("hello", contextualHandler.fromPayload(request, String.class, String.class));

    Payload result = contextualHandler.toPayload("reply").orElseThrow();
    assertEncryptionMetadata(result, endpoint, keyId);
    assertEncryptedWithKey(result, key);
    assertEquals("reply", caller.fromPayload(result, String.class, String.class));
  }

  private static void assertCompressionReducesPayload(
      String compressedEndpoint, String encryptedOnlyEndpoint) {
    String message = "repeat me ".repeat(100);
    Payload compressed = converterFor(compressedEndpoint).toPayload(message).orElseThrow();
    Payload encrypted = converterFor(encryptedOnlyEndpoint).toPayload(message).orElseThrow();
    assertTrue(compressed.getData().size() < encrypted.getData().size());
  }

  private static DataConverter converterFor(String endpoint) {
    return CallerWorker.dataConverter().withContext(contextFor(endpoint));
  }

  private static NexusSerializationContext contextFor(String endpoint) {
    // Supply context directly to check sample configuration, not SDK context propagation.
    return new NexusSerializationContext(
        endpoint, SyncEchoService.SERVICE_NAME, SyncEchoService.ECHO_OPERATION_NAME);
  }

  private static void assertEncryptionMetadata(Payload payload, String endpoint, String keyId) {
    assertEquals(
        NexusEncoding.AES_GCM.encodingName(),
        payload.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8());
    assertEquals(keyId, payload.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertEquals(
        endpoint, payload.getMetadataOrThrow(SampleConfig.ENDPOINT_METADATA_KEY).toStringUtf8());
  }

  private static void assertEncryptedWithKey(Payload payload, String key) throws Exception {
    byte[] encrypted = payload.getData().toByteArray();
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(
        Cipher.DECRYPT_MODE,
        new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
        new GCMParameterSpec(128, Arrays.copyOf(encrypted, 12)));
    Payload plaintext =
        Payload.parseFrom(cipher.doFinal(Arrays.copyOfRange(encrypted, 12, encrypted.length)));
    assertFalse(plaintext.getData().isEmpty());
  }
}
