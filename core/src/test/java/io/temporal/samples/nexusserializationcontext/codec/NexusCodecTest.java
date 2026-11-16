package io.temporal.samples.nexusserializationcontext.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.Test;

class NexusCodecTest {
  @Test
  void callerAndHandlersUseTheKeyForEachEndpoint() {
    DataConverter compressedEncryptedHandler = CompressedEncryptedHandlerWorker.dataConverter();
    DataConverter encryptedHandler = EncryptedHandlerWorker.dataConverter();

    assertEndpointRoundTrip(
        compressedEncryptedHandler,
        SampleConfig.COMPRESSED_ENCRYPTED_1_ENDPOINT,
        SampleConfig.COMPRESSED_ENCRYPTED_KEY_1_ID);
    assertEndpointRoundTrip(
        compressedEncryptedHandler,
        SampleConfig.COMPRESSED_ENCRYPTED_2_ENDPOINT,
        SampleConfig.COMPRESSED_ENCRYPTED_KEY_2_ID);
    assertEndpointRoundTrip(
        encryptedHandler, SampleConfig.ENCRYPTED_1_ENDPOINT, SampleConfig.ENCRYPTED_KEY_1_ID);
    assertEndpointRoundTrip(
        encryptedHandler, SampleConfig.ENCRYPTED_2_ENDPOINT, SampleConfig.ENCRYPTED_KEY_2_ID);
  }

  @Test
  void compressesBeforeEncryptingWithEitherKey() {
    assertCompressionReducesPayload(
        SampleConfig.COMPRESSED_ENCRYPTED_1_ENDPOINT, SampleConfig.ENCRYPTED_1_ENDPOINT);
    assertCompressionReducesPayload(
        SampleConfig.COMPRESSED_ENCRYPTED_2_ENDPOINT, SampleConfig.ENCRYPTED_2_ENDPOINT);
  }

  @Test
  void callerWorkflowPayloadsUseTheirOwnKey() {
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
    assertEquals("Hello from Nexus", converter.fromPayload(input, String.class, String.class));
    assertEquals(
        results, converter.fromPayload(result, EndpointResults.class, EndpointResults.class));
  }

  @Test
  void rejectsUnknownEndpoints() {
    DataConverter converter = converterFor("unknown");
    assertThrows(PayloadCodecException.class, () -> converter.toPayload("hello"));
  }

  private static void assertEndpointRoundTrip(
      DataConverter handler, String endpoint, String keyId) {
    DataConverter caller = converterFor(endpoint);
    DataConverter contextualHandler = handler.withContext(contextFor(endpoint));
    Payload request = caller.toPayload("hello").orElseThrow();
    assertEncryptionMetadata(request, endpoint, keyId);
    assertEquals("hello", contextualHandler.fromPayload(request, String.class, String.class));

    Payload result = contextualHandler.toPayload("reply").orElseThrow();
    assertEncryptionMetadata(result, endpoint, keyId);
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
}
