package io.temporal.samples.nexusserializationcontext.caller;

import io.temporal.samples.nexusserializationcontext.SampleConfig;
import io.temporal.samples.nexusserializationcontext.service.AsyncEchoService;
import io.temporal.samples.nexusserializationcontext.service.SyncEchoService;
import io.temporal.workflow.NexusOperationHandle;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class CallerWorkflowImpl implements CallerWorkflow {
  @Override
  public EndpointResults echoThroughEndpoints(String message) {
    SyncEchoService compressedEncryptedService =
        serviceFor(SyncEchoService.class, SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT);
    SyncEchoService encryptedService =
        serviceFor(SyncEchoService.class, SampleConfig.ENCRYPTED_ENDPOINT);
    AsyncEchoService compressedEncryptedAsyncService =
        serviceFor(AsyncEchoService.class, SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT);
    AsyncEchoService encryptedAsyncService =
        serviceFor(AsyncEchoService.class, SampleConfig.ENCRYPTED_ENDPOINT);

    // Start all operations before awaiting results. Each result keeps its endpoint context.
    NexusOperationHandle<String> compressedEncryptedSync =
        Workflow.startNexusOperation(compressedEncryptedService::echo, message);
    NexusOperationHandle<String> encryptedSync =
        Workflow.startNexusOperation(encryptedService::echo, message);
    NexusOperationHandle<String> compressedEncryptedAsync =
        Workflow.startNexusOperation(compressedEncryptedAsyncService::echoAsync, message);
    NexusOperationHandle<String> encryptedAsync =
        Workflow.startNexusOperation(encryptedAsyncService::echoAsync, message);
    return new EndpointResults(
        compressedEncryptedSync.getResult().get(),
        encryptedSync.getResult().get(),
        compressedEncryptedAsync.getResult().get(),
        encryptedAsync.getResult().get());
  }

  private static <T> T serviceFor(Class<T> serviceClass, String endpoint) {
    return Workflow.newNexusServiceStub(
        serviceClass,
        NexusServiceOptions.newBuilder()
            .setEndpoint(endpoint)
            .setOperationOptions(
                NexusOperationOptions.newBuilder()
                    .setScheduleToCloseTimeout(Duration.ofSeconds(30))
                    .build())
            .build());
  }
}
