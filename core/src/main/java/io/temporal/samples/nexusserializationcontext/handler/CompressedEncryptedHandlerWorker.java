package io.temporal.samples.nexusserializationcontext.handler;

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

public class CompressedEncryptedHandlerWorker {
  private static final String TASK_QUEUE = "nexus-serialization-compressed-encrypted-handler";

  public static void main(String[] args) {
    WorkflowClient client =
        ClientOptions.getWorkflowClient(
            args, WorkflowClientOptions.newBuilder().setDataConverter(dataConverter()));
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(EchoWorkflowImpl.class);
    worker.registerNexusServiceImplementation(new SyncEchoServiceImpl());
    worker.registerNexusServiceImplementation(new AsyncEchoServiceImpl());
    factory.start();
  }

  public static DataConverter dataConverter() {
    SecretKey key1 =
        new SecretKeySpec(
            SampleConfig.COMPRESSED_ENCRYPTED_KEY_1.getBytes(StandardCharsets.UTF_8), "AES");
    SecretKey key2 =
        new SecretKeySpec(
            SampleConfig.COMPRESSED_ENCRYPTED_KEY_2.getBytes(StandardCharsets.UTF_8), "AES");
    PayloadCodec key1Encryption = new AesGcmCodec(SampleConfig.COMPRESSED_ENCRYPTED_KEY_1_ID, key1);
    PayloadCodec key2Encryption = new AesGcmCodec(SampleConfig.COMPRESSED_ENCRYPTED_KEY_2_ID, key2);
    // ChainCodec encodes last to first: compress, then encrypt.
    PayloadCodec compressedKey1Encryption =
        new ChainCodec(List.of(key1Encryption, new ZlibCodec()));
    PayloadCodec compressedKey2Encryption =
        new ChainCodec(List.of(key2Encryption, new ZlibCodec()));
    Map<String, PayloadCodec> nexusEndpointCodecs =
        Map.of(
            SampleConfig.COMPRESSED_ENCRYPTED_1_ENDPOINT, compressedKey1Encryption,
            SampleConfig.COMPRESSED_ENCRYPTED_2_ENDPOINT, compressedKey2Encryption);
    // Ordinary workflow inputs use this handler's key 1, regardless of the Nexus endpoint.
    PayloadCodec handlerWorkflowEncryption = key1Encryption;
    return new CodecDataConverter(
        DefaultDataConverter.newDefaultInstance(),
        List.of(new NexusCodec(nexusEndpointCodecs, handlerWorkflowEncryption)));
  }
}
