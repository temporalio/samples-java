package io.temporal.samples.nexusserializationcontext.caller;

import io.temporal.samples.nexusserializationcontext.SampleConfig;
import io.temporal.samples.nexusserializationcontext.service.EchoService;
import io.temporal.workflow.NexusOperationHandle;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class CallerWorkflowImpl implements CallerWorkflow {
  @Override
  public EndpointResults echoThroughEndpoints(String message) {
    EchoService compressedEncryptedService = serviceFor(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT);
    EchoService encryptedService = serviceFor(SampleConfig.ENCRYPTED_ENDPOINT);
    EchoService asyncEncryptedService = serviceFor(SampleConfig.ASYNC_ENCRYPTED_ENDPOINT);
    EchoService asyncCompressedEncryptedService =
        serviceFor(SampleConfig.ASYNC_COMPRESSED_ENCRYPTED_ENDPOINT);

    // Start all operations before awaiting results. Each result keeps its endpoint context.
    NexusOperationHandle<String> compressedEncryptedSync =
        Workflow.startNexusOperation(compressedEncryptedService::echo, message);
    NexusOperationHandle<String> encryptedSync =
        Workflow.startNexusOperation(encryptedService::echo, message);
    NexusOperationHandle<String> asyncEncrypted =
        Workflow.startNexusOperation(asyncEncryptedService::echoAsync, message);
    NexusOperationHandle<String> asyncCompressedEncrypted =
        Workflow.startNexusOperation(asyncCompressedEncryptedService::echoAsync, message);
    return new EndpointResults(
        compressedEncryptedSync.getResult().get(),
        encryptedSync.getResult().get(),
        asyncEncrypted.getResult().get(),
        asyncCompressedEncrypted.getResult().get());
  }

  private static EchoService serviceFor(String endpoint) {
    return Workflow.newNexusServiceStub(
        EchoService.class,
        NexusServiceOptions.newBuilder()
            .setEndpoint(endpoint)
            .setOperationOptions(
                NexusOperationOptions.newBuilder()
                    .setScheduleToCloseTimeout(Duration.ofSeconds(30))
                    .build())
            .build());
  }
}
