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
import io.temporal.samples.nexusserializationcontext.codec.PropagatedEndpointCodec;
import io.temporal.samples.nexusserializationcontext.codec.ZlibCodec;
import io.temporal.samples.nexusserializationcontext.propagation.NexusEndpointContextPropagator;
import io.temporal.samples.nexusserializationcontext.propagation.NexusEndpointInterceptor;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AsyncHandlerWorker {
  private static final String TASK_QUEUE = "nexus-serialization-async-handler";

  public static void main(String[] args) {
    WorkflowClient client =
        ClientOptions.getWorkflowClient(
            args,
            WorkflowClientOptions.newBuilder()
                .setDataConverter(dataConverter())
                .setContextPropagators(List.of(new NexusEndpointContextPropagator())));
    WorkerFactory factory =
        WorkerFactory.newInstance(
            client,
            WorkerFactoryOptions.newBuilder()
                .setWorkerInterceptors(new NexusEndpointInterceptor())
                .build());
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(EchoWorkflowImpl.class);
    worker.registerNexusServiceImplementation(new EchoServiceImpl());
    factory.start();
  }

  public static DataConverter dataConverter() {
    SecretKey keyC =
        new SecretKeySpec(SampleConfig.KEY_C_VALUE.getBytes(StandardCharsets.UTF_8), "AES");
    SecretKey keyD =
        new SecretKeySpec(SampleConfig.KEY_D_VALUE.getBytes(StandardCharsets.UTF_8), "AES");
    PayloadCodec encrypted = new AesGcmCodec(SampleConfig.KEY_C_ID, keyC);
    // ChainCodec encodes last to first: compress, then encrypt.
    PayloadCodec compressedEncrypted =
        new ChainCodec(List.of(new AesGcmCodec(SampleConfig.KEY_D_ID, keyD), new ZlibCodec()));
    return new CodecDataConverter(
        DefaultDataConverter.newDefaultInstance(),
        List.of(
            new PropagatedEndpointCodec(
                Map.of(
                    SampleConfig.ASYNC_ENCRYPTED_ENDPOINT,
                    encrypted,
                    SampleConfig.ASYNC_COMPRESSED_ENCRYPTED_ENDPOINT,
                    compressedEncrypted))));
  }
}
