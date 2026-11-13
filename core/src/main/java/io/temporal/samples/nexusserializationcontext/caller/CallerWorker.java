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
    SecretKey handlerAKey1 =
        new SecretKeySpec(SampleConfig.HANDLER_A_KEY_1.getBytes(StandardCharsets.UTF_8), "AES");
    SecretKey handlerBKey1 =
        new SecretKeySpec(SampleConfig.HANDLER_B_KEY_1.getBytes(StandardCharsets.UTF_8), "AES");
    SecretKey handlerAKey2 =
        new SecretKeySpec(SampleConfig.HANDLER_A_KEY_2.getBytes(StandardCharsets.UTF_8), "AES");
    SecretKey handlerBKey2 =
        new SecretKeySpec(SampleConfig.HANDLER_B_KEY_2.getBytes(StandardCharsets.UTF_8), "AES");
    PayloadCodec handlerAKey1Encryption =
        new AesGcmCodec(SampleConfig.HANDLER_A_KEY_1_ID, handlerAKey1);
    PayloadCodec handlerBKey1Encryption =
        new AesGcmCodec(SampleConfig.HANDLER_B_KEY_1_ID, handlerBKey1);
    PayloadCodec handlerAKey2Encryption =
        new AesGcmCodec(SampleConfig.HANDLER_A_KEY_2_ID, handlerAKey2);
    PayloadCodec handlerBKey2Encryption =
        new AesGcmCodec(SampleConfig.HANDLER_B_KEY_2_ID, handlerBKey2);
    // ChainCodec encodes last to first: compress, then encrypt.
    Map<String, PayloadCodec> nexusEndpointCodecs =
        Map.of(
            SampleConfig.HANDLER_A_1_ENDPOINT,
            new ChainCodec(List.of(handlerAKey1Encryption, new ZlibCodec())),
            SampleConfig.HANDLER_B_1_ENDPOINT,
            handlerBKey1Encryption,
            SampleConfig.HANDLER_A_2_ENDPOINT,
            new ChainCodec(List.of(handlerAKey2Encryption, new ZlibCodec())),
            SampleConfig.HANDLER_B_2_ENDPOINT,
            handlerBKey2Encryption);
    // Reuse handler A's key 1 for the caller's workflow payloads.
    PayloadCodec callerWorkflowEncryption = handlerAKey1Encryption;
    return new CodecDataConverter(
        DefaultDataConverter.newDefaultInstance(),
        List.of(new NexusCodec(nexusEndpointCodecs, callerWorkflowEncryption)));
  }
}
