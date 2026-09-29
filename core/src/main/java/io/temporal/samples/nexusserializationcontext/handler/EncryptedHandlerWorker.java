package io.temporal.samples.nexusserializationcontext.handler;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.samples.nexusserializationcontext.AesGcmCodec;
import io.temporal.samples.nexusserializationcontext.SampleConfig;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptedHandlerWorker {
  private static final String TASK_QUEUE = "nexus-serialization-key-b-handler";

  public static void main(String[] args) {
    WorkflowClient client =
        ClientOptions.getWorkflowClient(
            args, WorkflowClientOptions.newBuilder().setDataConverter(dataConverter()));
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerNexusServiceImplementation(new EchoServiceImpl());
    factory.start();
  }

  public static DataConverter dataConverter() {
    SecretKey keyB =
        new SecretKeySpec(SampleConfig.KEY_B_VALUE.getBytes(StandardCharsets.UTF_8), "AES");
    return new CodecDataConverter(
        DefaultDataConverter.newDefaultInstance(),
        List.of(new AesGcmCodec(SampleConfig.KEY_B_ID, keyB)));
  }
}
