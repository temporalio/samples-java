package io.temporal.samples.nexusserializationcontext.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.payload.codec.PayloadCodecException;
import io.temporal.payload.context.NexusSerializationContext;
import io.temporal.payload.context.SerializationContext;
import io.temporal.payload.context.WorkflowSerializationContext;
import io.temporal.samples.nexusserializationcontext.SampleConfig;
import io.temporal.samples.nexusserializationcontext.caller.CallerWorker;
import io.temporal.samples.nexusserializationcontext.handler.AsyncHandlerWorker;
import io.temporal.samples.nexusserializationcontext.handler.CompressedEncryptedHandlerWorker;
import io.temporal.samples.nexusserializationcontext.handler.EncryptedHandlerWorker;
import io.temporal.samples.nexusserializationcontext.propagation.NexusEndpointContextPropagator;
import io.temporal.samples.nexusserializationcontext.service.EchoService;
import org.junit.jupiter.api.Test;

class NexusCodecTest {
  @Test
  void encryptsWithTheKeyForEachEndpoint() {
    DataConverter keyAConverter = converterFor(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT);
    DataConverter keyBConverter = converterFor(SampleConfig.ENCRYPTED_ENDPOINT);
    DataConverter keyCConverter = converterFor(SampleConfig.ASYNC_ENCRYPTED_ENDPOINT);
    DataConverter keyDConverter = converterFor(SampleConfig.ASYNC_COMPRESSED_ENCRYPTED_ENDPOINT);

    Payload keyAPayload = keyAConverter.toPayload("hello").orElseThrow();
    Payload keyBPayload = keyBConverter.toPayload("hello").orElseThrow();
    Payload keyCPayload = keyCConverter.toPayload("hello").orElseThrow();
    Payload keyDPayload = keyDConverter.toPayload("hello").orElseThrow();

    assertEquals(
        NexusEncoding.AES_GCM.encodingName(),
        keyAPayload.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8());
    assertEquals(
        NexusEncoding.AES_GCM.encodingName(),
        keyBPayload.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8());
    assertEquals(
        NexusEncoding.AES_GCM.encodingName(),
        keyCPayload.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8());
    assertEquals(
        NexusEncoding.AES_GCM.encodingName(),
        keyDPayload.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.KEY_A_ID,
        keyAPayload.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.KEY_B_ID,
        keyBPayload.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.KEY_C_ID,
        keyCPayload.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.KEY_D_ID,
        keyDPayload.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT,
        keyAPayload.getMetadataOrThrow(SampleConfig.ENDPOINT_METADATA_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.ENCRYPTED_ENDPOINT,
        keyBPayload.getMetadataOrThrow(SampleConfig.ENDPOINT_METADATA_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.ASYNC_ENCRYPTED_ENDPOINT,
        keyCPayload.getMetadataOrThrow(SampleConfig.ENDPOINT_METADATA_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.ASYNC_COMPRESSED_ENCRYPTED_ENDPOINT,
        keyDPayload.getMetadataOrThrow(SampleConfig.ENDPOINT_METADATA_KEY).toStringUtf8());
    assertEquals("hello", keyAConverter.fromPayload(keyAPayload, String.class, String.class));
    assertEquals("hello", keyBConverter.fromPayload(keyBPayload, String.class, String.class));
    assertEquals("hello", keyCConverter.fromPayload(keyCPayload, String.class, String.class));
    assertEquals("hello", keyDConverter.fromPayload(keyDPayload, String.class, String.class));
    assertThrows(
        PayloadCodecException.class,
        () -> keyAConverter.fromPayload(keyBPayload, String.class, String.class));
    assertThrows(
        PayloadCodecException.class,
        () -> keyBConverter.fromPayload(keyCPayload, String.class, String.class));
    assertThrows(
        PayloadCodecException.class,
        () -> keyCConverter.fromPayload(keyDPayload, String.class, String.class));
    Payload keyAPayloadMarkedAsB =
        keyAPayload.toBuilder()
            .putMetadata(
                AesGcmCodec.KEY_ID_METADATA_KEY, ByteString.copyFromUtf8(SampleConfig.KEY_B_ID))
            .build();
    assertThrows(
        PayloadCodecException.class,
        () -> keyBConverter.fromPayload(keyAPayloadMarkedAsB, String.class, String.class));
  }

  @Test
  void compressesBeforeEncryptingForTheFirstEndpoint() {
    String message = "repeat me ".repeat(100);
    Payload compressed =
        converterFor(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT).toPayload(message).orElseThrow();
    Payload encryptedOnly =
        converterFor(SampleConfig.ENCRYPTED_ENDPOINT).toPayload(message).orElseThrow();

    assertTrue(compressed.getData().size() < encryptedOnly.getData().size());

    Payload asyncCompressed =
        converterFor(SampleConfig.ASYNC_COMPRESSED_ENCRYPTED_ENDPOINT)
            .toPayload(message)
            .orElseThrow();
    Payload asyncEncrypted =
        converterFor(SampleConfig.ASYNC_ENCRYPTED_ENDPOINT).toPayload(message).orElseThrow();
    assertTrue(asyncCompressed.getData().size() < asyncEncrypted.getData().size());
  }

  @Test
  void leavesNonNexusPayloadsUnchanged() {
    DataConverter converter =
        CallerWorker.dataConverter().withContext(new SerializationContext() {});

    Payload payload = converter.toPayload("hello").orElseThrow();

    assertEquals(
        "json/plain",
        payload.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8());
    assertEquals("hello", converter.fromPayload(payload, String.class, String.class));
  }

  @Test
  void rejectsUnknownEndpoints() {
    DataConverter converter =
        CallerWorker.dataConverter()
            .withContext(
                new NexusSerializationContext(
                    "unknown", EchoService.SERVICE_NAME, EchoService.ECHO_OPERATION_NAME));

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
  void sharedAsyncHandlerDecodesRequestsUsingNexusContext() {
    DataConverter handlerC =
        AsyncHandlerWorker.dataConverter()
            .withContext(contextFor(SampleConfig.ASYNC_ENCRYPTED_ENDPOINT));
    DataConverter handlerD =
        AsyncHandlerWorker.dataConverter()
            .withContext(contextFor(SampleConfig.ASYNC_COMPRESSED_ENCRYPTED_ENDPOINT));
    Payload requestC =
        converterFor(SampleConfig.ASYNC_ENCRYPTED_ENDPOINT).toPayload("hello").orElseThrow();
    Payload requestD =
        converterFor(SampleConfig.ASYNC_COMPRESSED_ENCRYPTED_ENDPOINT)
            .toPayload("hello")
            .orElseThrow();

    assertEquals("hello", handlerC.fromPayload(requestC, String.class, String.class));
    assertEquals("hello", handlerD.fromPayload(requestD, String.class, String.class));
    assertThrows(
        PayloadCodecException.class,
        () -> handlerC.fromPayload(requestD, String.class, String.class));
  }

  @Test
  void propagatedEndpointSelectsAsyncWorkflowResultCodec() {
    NexusEndpointContextPropagator propagator = new NexusEndpointContextPropagator();
    try {
      propagator.setCurrentContext(null);
      DataConverter handler =
          AsyncHandlerWorker.dataConverter()
              .withContext(new WorkflowSerializationContext("handler-namespace", "echo-workflow"));
      assertThrows(PayloadCodecException.class, () -> handler.toPayload("reply"));

      Payload resultC =
          asyncResultFor(
              propagator, handler, SampleConfig.ASYNC_ENCRYPTED_ENDPOINT, SampleConfig.KEY_C_ID);
      Payload resultD =
          asyncResultFor(
              propagator,
              handler,
              SampleConfig.ASYNC_COMPRESSED_ENCRYPTED_ENDPOINT,
              SampleConfig.KEY_D_ID);
      assertTrue(resultD.getData().size() < resultC.getData().size());
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
    assertEquals(message, converterFor(endpoint).fromPayload(result, String.class, String.class));
    assertEquals(message, handler.fromPayload(result, String.class, String.class));
    propagator.setCurrentContext(null);
    return result;
  }

  private static DataConverter converterFor(String endpoint) {
    return CallerWorker.dataConverter().withContext(contextFor(endpoint));
  }

  private static NexusSerializationContext contextFor(String endpoint) {
    String operation =
        SampleConfig.ASYNC_ENCRYPTED_ENDPOINT.equals(endpoint)
                || SampleConfig.ASYNC_COMPRESSED_ENCRYPTED_ENDPOINT.equals(endpoint)
            ? EchoService.ECHO_ASYNC_OPERATION_NAME
            : EchoService.ECHO_OPERATION_NAME;
    return new NexusSerializationContext(endpoint, EchoService.SERVICE_NAME, operation);
  }
}
