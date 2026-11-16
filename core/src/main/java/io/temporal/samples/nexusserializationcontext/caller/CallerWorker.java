package io.temporal.samples.nexusserializationcontext.caller;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.payload.codec.ChainCodec;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.samples.nexusserializationcontext.SampleConfig;
import io.temporal.samples.nexusserializationcontext.codec.AesGcmCodec;
import io.temporal.samples.nexusserializationcontext.codec.NexusCodec;
import io.temporal.samples.nexusserializationcontext.codec.ZlibCodec;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CallerWorker {
  static final String TASK_QUEUE = "nexus-serialization-caller";

  public static void main(String[] args) {
    WorkflowClient client =
        ClientOptions.getWorkflowClient(
            args, WorkflowClientOptions.newBuilder().setDataConverter(dataConverter()));
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(CallerWorkflowImpl.class);
    factory.start();
  }

  public static DataConverter dataConverter() {
    SecretKey compressedEncryptedKey1 =
        new SecretKeySpec(
            SampleConfig.COMPRESSED_ENCRYPTED_KEY_1.getBytes(StandardCharsets.UTF_8), "AES");
    SecretKey encryptedKey1 =
        new SecretKeySpec(SampleConfig.ENCRYPTED_KEY_1.getBytes(StandardCharsets.UTF_8), "AES");
    SecretKey compressedEncryptedKey2 =
        new SecretKeySpec(
            SampleConfig.COMPRESSED_ENCRYPTED_KEY_2.getBytes(StandardCharsets.UTF_8), "AES");
    SecretKey encryptedKey2 =
        new SecretKeySpec(SampleConfig.ENCRYPTED_KEY_2.getBytes(StandardCharsets.UTF_8), "AES");
    SecretKey callerWorkflowKey =
        new SecretKeySpec(SampleConfig.CALLER_WORKFLOW_KEY.getBytes(StandardCharsets.UTF_8), "AES");
    PayloadCodec compressedEncryptedKey1Encryption =
        new AesGcmCodec(SampleConfig.COMPRESSED_ENCRYPTED_KEY_1_ID, compressedEncryptedKey1);
    PayloadCodec encryptedKey1Encryption =
        new AesGcmCodec(SampleConfig.ENCRYPTED_KEY_1_ID, encryptedKey1);
    PayloadCodec compressedEncryptedKey2Encryption =
        new AesGcmCodec(SampleConfig.COMPRESSED_ENCRYPTED_KEY_2_ID, compressedEncryptedKey2);
    PayloadCodec encryptedKey2Encryption =
        new AesGcmCodec(SampleConfig.ENCRYPTED_KEY_2_ID, encryptedKey2);
    // ChainCodec encodes last to first: compress, then encrypt.
    Map<String, PayloadCodec> nexusEndpointCodecs =
        Map.of(
            SampleConfig.COMPRESSED_ENCRYPTED_1_ENDPOINT,
            new ChainCodec(List.of(compressedEncryptedKey1Encryption, new ZlibCodec())),
            SampleConfig.ENCRYPTED_1_ENDPOINT,
            encryptedKey1Encryption,
            SampleConfig.COMPRESSED_ENCRYPTED_2_ENDPOINT,
            new ChainCodec(List.of(compressedEncryptedKey2Encryption, new ZlibCodec())),
            SampleConfig.ENCRYPTED_2_ENDPOINT,
            encryptedKey2Encryption);
    // Ordinary caller workflow payloads use a separate key from Nexus payloads.
    PayloadCodec callerWorkflowEncryption =
        new AesGcmCodec(SampleConfig.CALLER_WORKFLOW_KEY_ID, callerWorkflowKey);
    return new CodecDataConverter(
        DefaultDataConverter.newDefaultInstance(),
        List.of(new NexusCodec(nexusEndpointCodecs, callerWorkflowEncryption)));
  }
}
