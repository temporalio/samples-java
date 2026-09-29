package io.temporal.samples.nexusserializationcontext;

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
import io.temporal.samples.nexusserializationcontext.caller.CallerWorker;
import io.temporal.samples.nexusserializationcontext.handler.CompressedEncryptedHandlerWorker;
import io.temporal.samples.nexusserializationcontext.handler.EncryptedHandlerWorker;
import io.temporal.samples.nexusserializationcontext.service.EchoService;
import org.junit.jupiter.api.Test;

class NexusCodecTest {
  @Test
  void encryptsWithTheKeyForEachEndpoint() {
    DataConverter keyAConverter = converterFor(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT);
    DataConverter keyBConverter = converterFor(SampleConfig.ENCRYPTED_ENDPOINT);

    Payload keyAPayload = keyAConverter.toPayload("hello").orElseThrow();
    Payload keyBPayload = keyBConverter.toPayload("hello").orElseThrow();

    assertEquals(
        NexusEncoding.AES_GCM.encodingName(),
        keyAPayload.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8());
    assertEquals(
        NexusEncoding.AES_GCM.encodingName(),
        keyBPayload.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.KEY_A_ID,
        keyAPayload.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertEquals(
        SampleConfig.KEY_B_ID,
        keyBPayload.getMetadataOrThrow(AesGcmCodec.KEY_ID_METADATA_KEY).toStringUtf8());
    assertEquals("hello", keyAConverter.fromPayload(keyAPayload, String.class, String.class));
    assertEquals("hello", keyBConverter.fromPayload(keyBPayload, String.class, String.class));
    assertThrows(
        PayloadCodecException.class,
        () -> keyAConverter.fromPayload(keyBPayload, String.class, String.class));
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
  void eachHandlerUsesItsOwnCodecWithoutEndpointContext() {
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

    assertThrows(
        PayloadCodecException.class,
        () -> handlerA.fromPayload(requestB, String.class, String.class));
    assertThrows(
        PayloadCodecException.class,
        () -> handlerB.fromPayload(requestA, String.class, String.class));
  }

  private static DataConverter converterFor(String endpoint) {
    return CallerWorker.dataConverter().withContext(contextFor(endpoint));
  }

  private static NexusSerializationContext contextFor(String endpoint) {
    return new NexusSerializationContext(
        endpoint, EchoService.SERVICE_NAME, EchoService.ECHO_OPERATION_NAME);
  }
}
