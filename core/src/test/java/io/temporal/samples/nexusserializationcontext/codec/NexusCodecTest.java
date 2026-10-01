package io.temporal.samples.nexusserializationcontext.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import io.temporal.samples.nexusserializationcontext.propagation.NexusEndpointContextPropagator;
import io.temporal.samples.nexusserializationcontext.service.AsyncEchoService;
import io.temporal.samples.nexusserializationcontext.service.SyncEchoService;
import org.junit.jupiter.api.Test;

class NexusCodecTest {
  @Test
  void encryptsSyncAndAsyncOperationsWithTheKeyForTheirEndpoint() {
    DataConverter syncA = converterFor(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT);
    DataConverter asyncA = converterFor(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT, true);
    DataConverter syncB = converterFor(SampleConfig.ENCRYPTED_ENDPOINT);
    DataConverter asyncB = converterFor(SampleConfig.ENCRYPTED_ENDPOINT, true);

    Payload syncAPayload = syncA.toPayload("hello").orElseThrow();
    Payload asyncAPayload = asyncA.toPayload("hello").orElseThrow();
    Payload syncBPayload = syncB.toPayload("hello").orElseThrow();
    Payload asyncBPayload = asyncB.toPayload("hello").orElseThrow();

    assertEncryptedFor(
        syncAPayload, SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT, SampleConfig.KEY_A_ID);
    assertEncryptedFor(
        asyncAPayload, SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT, SampleConfig.KEY_A_ID);
    assertEncryptedFor(syncBPayload, SampleConfig.ENCRYPTED_ENDPOINT, SampleConfig.KEY_B_ID);
    assertEncryptedFor(asyncBPayload, SampleConfig.ENCRYPTED_ENDPOINT, SampleConfig.KEY_B_ID);
    assertEquals("hello", syncA.fromPayload(syncAPayload, String.class, String.class));
    assertEquals("hello", asyncA.fromPayload(asyncAPayload, String.class, String.class));
    assertEquals("hello", syncB.fromPayload(syncBPayload, String.class, String.class));
    assertEquals("hello", asyncB.fromPayload(asyncBPayload, String.class, String.class));
    assertThrows(
        PayloadCodecException.class,
        () -> syncA.fromPayload(syncBPayload, String.class, String.class));
    assertThrows(
        PayloadCodecException.class,
        () -> asyncB.fromPayload(asyncAPayload, String.class, String.class));
    Payload keyAPayloadMarkedAsB =
        syncAPayload.toBuilder()
            .putMetadata(
                AesGcmCodec.KEY_ID_METADATA_KEY, ByteString.copyFromUtf8(SampleConfig.KEY_B_ID))
            .build();
    assertThrows(
        PayloadCodecException.class,
        () -> syncB.fromPayload(keyAPayloadMarkedAsB, String.class, String.class));
  }

  @Test
  void compressesBeforeEncryptingForBothOperationsOnTheCompressedEndpoint() {
    String message = "repeat me ".repeat(100);
    Payload syncCompressed =
        converterFor(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT).toPayload(message).orElseThrow();
    Payload syncEncryptedOnly =
        converterFor(SampleConfig.ENCRYPTED_ENDPOINT).toPayload(message).orElseThrow();
    Payload asyncCompressed =
        converterFor(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT, true)
            .toPayload(message)
            .orElseThrow();
    Payload asyncEncrypted =
        converterFor(SampleConfig.ENCRYPTED_ENDPOINT, true).toPayload(message).orElseThrow();

    assertTrue(syncCompressed.getData().size() < syncEncryptedOnly.getData().size());
    assertTrue(asyncCompressed.getData().size() < asyncEncrypted.getData().size());
  }

  @Test
  void encryptsCallerWorkflowInputAndResultWithKeyC() {
    DataConverter converter =
        CallerWorker.dataConverter()
            .withContext(
                new WorkflowSerializationContext("nexus-serialization-caller", "caller-id"));
    EndpointResults results = new EndpointResults("reply A", "reply B", "reply C", "reply D");

    Payload input = converter.toPayload("Hello from Nexus").orElseThrow();
    Payload result = converter.toPayload(results).orElseThrow();

    assertEquals(
        NexusEncoding.AES_GCM.encodingName(),
        input.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.KEY_C_ID,
        input.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.KEY_C_ID,
        result.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertFalse(input.getMetadataMap().containsKey(SampleConfig.ENDPOINT_METADATA_KEY));
    assertFalse(input.getData().toStringUtf8().contains("Hello from Nexus"));
    assertFalse(result.getData().toStringUtf8().contains("reply A"));
    assertEquals("Hello from Nexus", converter.fromPayload(input, String.class, String.class));
    assertEquals(
        results, converter.fromPayload(result, EndpointResults.class, EndpointResults.class));
  }

  @Test
  void rejectsUnknownEndpoints() {
    DataConverter converter =
        CallerWorker.dataConverter()
            .withContext(
                new NexusSerializationContext(
                    "unknown", SyncEchoService.SERVICE_NAME, SyncEchoService.ECHO_OPERATION_NAME));

    assertThrows(PayloadCodecException.class, () -> converter.toPayload("hello"));
  }

  @Test
  void synchronousHandlersUseTheirOwnCodecWithoutEndpointContext() {
    DataConverter callerA = converterFor(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT);
    DataConverter callerB = converterFor(SampleConfig.ENCRYPTED_ENDPOINT);
    DataConverter handlerA = CompressedEncryptedHandlerWorker.dataConverter();
    DataConverter handlerB = EncryptedHandlerWorker.dataConverter();

    Payload requestA = callerA.toPayload("hello").orElseThrow();
    Payload requestB = callerB.toPayload("hello").orElseThrow();
    assertEquals("hello", handlerA.fromPayload(requestA, String.class, String.class));
    assertEquals("hello", handlerB.fromPayload(requestB, String.class, String.class));

    Payload resultA = handlerA.toPayload("reply").orElseThrow();
    Payload resultB = handlerB.toPayload("reply").orElseThrow();
    assertEquals("reply", callerA.fromPayload(resultA, String.class, String.class));
    assertEquals("reply", callerB.fromPayload(resultB, String.class, String.class));

    Payload contextualResultA =
        handlerA
            .withContext(contextFor(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT))
            .toPayload("reply")
            .orElseThrow();
    Payload contextualResultB =
        handlerB
            .withContext(contextFor(SampleConfig.ENCRYPTED_ENDPOINT))
            .toPayload("reply")
            .orElseThrow();
    assertEquals(
        SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT,
        contextualResultA.getMetadataOrThrow(SampleConfig.ENDPOINT_METADATA_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.ENCRYPTED_ENDPOINT,
        contextualResultB.getMetadataOrThrow(SampleConfig.ENDPOINT_METADATA_KEY).toStringUtf8());

    assertThrows(
        PayloadCodecException.class,
        () -> handlerA.fromPayload(requestB, String.class, String.class));
    assertThrows(
        PayloadCodecException.class,
        () -> handlerB.fromPayload(requestA, String.class, String.class));
  }

  @Test
  void handlersDecodeAsyncRequestsWithTheirEndpointKey() {
    DataConverter handlerA =
        CompressedEncryptedHandlerWorker.dataConverter()
            .withContext(contextFor(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT, true));
    DataConverter handlerB =
        EncryptedHandlerWorker.dataConverter()
            .withContext(contextFor(SampleConfig.ENCRYPTED_ENDPOINT, true));
    Payload requestA =
        converterFor(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT, true)
            .toPayload("hello")
            .orElseThrow();
    Payload requestB =
        converterFor(SampleConfig.ENCRYPTED_ENDPOINT, true).toPayload("hello").orElseThrow();

    assertEquals("hello", handlerA.fromPayload(requestA, String.class, String.class));
    assertEquals("hello", handlerB.fromPayload(requestB, String.class, String.class));
    assertThrows(
        PayloadCodecException.class,
        () -> handlerA.fromPayload(requestB, String.class, String.class));
  }

  @Test
  void propagatedEndpointAppearsOnEncryptedAsyncWorkflowResult() {
    NexusEndpointContextPropagator propagator = new NexusEndpointContextPropagator();
    try {
      propagator.setCurrentContext(null);
      DataConverter handlerA =
          CompressedEncryptedHandlerWorker.dataConverter()
              .withContext(new WorkflowSerializationContext("handler-a", "echo-workflow"));
      DataConverter handlerB =
          EncryptedHandlerWorker.dataConverter()
              .withContext(new WorkflowSerializationContext("handler-b", "echo-workflow"));

      Payload resultA =
          asyncResultFor(
              propagator,
              handlerA,
              SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT,
              SampleConfig.KEY_A_ID);
      Payload resultB =
          asyncResultFor(
              propagator, handlerB, SampleConfig.ENCRYPTED_ENDPOINT, SampleConfig.KEY_B_ID);
      assertTrue(resultA.getData().size() < resultB.getData().size());
    } finally {
      propagator.setCurrentContext(null);
    }
  }

  private static Payload asyncResultFor(
      NexusEndpointContextPropagator propagator,
      DataConverter handler,
      String endpoint,
      String keyId) {
    propagator.setCurrentContext(endpoint);
    var header = propagator.serializeContext(propagator.getCurrentContext());
    propagator.setCurrentContext(null);
    propagator.setCurrentContext(propagator.deserializeContext(header));

    String message = "reply ".repeat(100);
    Payload result = handler.toPayload(message).orElseThrow();
    assertEquals(
        endpoint, result.getMetadataOrThrow(SampleConfig.ENDPOINT_METADATA_KEY).toStringUtf8());
    assertEquals(keyId, result.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertEquals(
        message, converterFor(endpoint, true).fromPayload(result, String.class, String.class));
    assertEquals(message, handler.fromPayload(result, String.class, String.class));
    propagator.setCurrentContext(null);
    return result;
  }

  private static DataConverter converterFor(String endpoint) {
    return CallerWorker.dataConverter().withContext(contextFor(endpoint));
  }

  private static DataConverter converterFor(String endpoint, boolean async) {
    return CallerWorker.dataConverter().withContext(contextFor(endpoint, async));
  }

  private static NexusSerializationContext contextFor(String endpoint) {
    return contextFor(endpoint, false);
  }

  private static NexusSerializationContext contextFor(String endpoint, boolean async) {
    if (async) {
      return new NexusSerializationContext(
          endpoint, AsyncEchoService.SERVICE_NAME, AsyncEchoService.ECHO_ASYNC_OPERATION_NAME);
    }
    return new NexusSerializationContext(
        endpoint, SyncEchoService.SERVICE_NAME, SyncEchoService.ECHO_OPERATION_NAME);
  }

  private static void assertEncryptedFor(Payload payload, String endpoint, String keyId) {
    assertEquals(
        NexusEncoding.AES_GCM.encodingName(),
        payload.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8());
    assertEquals(keyId, payload.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertEquals(
        endpoint, payload.getMetadataOrThrow(SampleConfig.ENDPOINT_METADATA_KEY).toStringUtf8());
  }
}
