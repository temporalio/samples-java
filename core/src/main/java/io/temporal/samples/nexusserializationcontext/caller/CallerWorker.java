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
    SecretKey keyA =
        new SecretKeySpec(SampleConfig.KEY_A_VALUE.getBytes(StandardCharsets.UTF_8), "AES");
    SecretKey keyB =
        new SecretKeySpec(SampleConfig.KEY_B_VALUE.getBytes(StandardCharsets.UTF_8), "AES");
    SecretKey keyC =
        new SecretKeySpec(SampleConfig.KEY_C_VALUE.getBytes(StandardCharsets.UTF_8), "AES");
    SecretKey keyD =
        new SecretKeySpec(SampleConfig.KEY_D_VALUE.getBytes(StandardCharsets.UTF_8), "AES");
    SecretKey keyE =
        new SecretKeySpec(SampleConfig.KEY_E_VALUE.getBytes(StandardCharsets.UTF_8), "AES");
    // ChainCodec encodes last to first: compress, then encrypt.
    PayloadCodec compressedEncrypted =
        new ChainCodec(List.of(new AesGcmCodec(SampleConfig.KEY_A_ID, keyA), new ZlibCodec()));
    PayloadCodec encrypted = new AesGcmCodec(SampleConfig.KEY_B_ID, keyB);
    PayloadCodec asyncEncrypted = new AesGcmCodec(SampleConfig.KEY_C_ID, keyC);
    PayloadCodec asyncCompressedEncrypted =
        new ChainCodec(List.of(new AesGcmCodec(SampleConfig.KEY_D_ID, keyD), new ZlibCodec()));
    return new CodecDataConverter(
        DefaultDataConverter.newDefaultInstance(),
        List.of(
            new NexusCodec(
                Map.of(
                    SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT,
                    compressedEncrypted,
                    SampleConfig.ENCRYPTED_ENDPOINT,
                    encrypted,
                    SampleConfig.ASYNC_ENCRYPTED_ENDPOINT,
                    asyncEncrypted,
                    SampleConfig.ASYNC_COMPRESSED_ENCRYPTED_ENDPOINT,
                    asyncCompressedEncrypted),
                new AesGcmCodec(SampleConfig.KEY_E_ID, keyE))));
  }
}
