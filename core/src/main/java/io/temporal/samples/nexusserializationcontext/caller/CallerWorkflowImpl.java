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
    SyncEchoService compressedEncrypted1SyncService =
        serviceFor(SyncEchoService.class, SampleConfig.COMPRESSED_ENCRYPTED_1_ENDPOINT);
    SyncEchoService encrypted1SyncService =
        serviceFor(SyncEchoService.class, SampleConfig.ENCRYPTED_1_ENDPOINT);
    SyncEchoService compressedEncrypted2SyncService =
        serviceFor(SyncEchoService.class, SampleConfig.COMPRESSED_ENCRYPTED_2_ENDPOINT);
    SyncEchoService encrypted2SyncService =
        serviceFor(SyncEchoService.class, SampleConfig.ENCRYPTED_2_ENDPOINT);
    AsyncEchoService compressedEncrypted1AsyncService =
        serviceFor(AsyncEchoService.class, SampleConfig.COMPRESSED_ENCRYPTED_1_ENDPOINT);
    AsyncEchoService encrypted1AsyncService =
        serviceFor(AsyncEchoService.class, SampleConfig.ENCRYPTED_1_ENDPOINT);
    AsyncEchoService compressedEncrypted2AsyncService =
        serviceFor(AsyncEchoService.class, SampleConfig.COMPRESSED_ENCRYPTED_2_ENDPOINT);
    AsyncEchoService encrypted2AsyncService =
        serviceFor(AsyncEchoService.class, SampleConfig.ENCRYPTED_2_ENDPOINT);

    // Start all eight operations before awaiting results. Each keeps its endpoint context.
    NexusOperationHandle<String> compressedEncrypted1Sync =
        Workflow.startNexusOperation(compressedEncrypted1SyncService::echo, message);
    NexusOperationHandle<String> compressedEncrypted1Async =
        Workflow.startNexusOperation(compressedEncrypted1AsyncService::echoAsync, message);
    NexusOperationHandle<String> encrypted1Sync =
        Workflow.startNexusOperation(encrypted1SyncService::echo, message);
    NexusOperationHandle<String> encrypted1Async =
        Workflow.startNexusOperation(encrypted1AsyncService::echoAsync, message);
    NexusOperationHandle<String> compressedEncrypted2Sync =
        Workflow.startNexusOperation(compressedEncrypted2SyncService::echo, message);
    NexusOperationHandle<String> compressedEncrypted2Async =
        Workflow.startNexusOperation(compressedEncrypted2AsyncService::echoAsync, message);
    NexusOperationHandle<String> encrypted2Sync =
        Workflow.startNexusOperation(encrypted2SyncService::echo, message);
    NexusOperationHandle<String> encrypted2Async =
        Workflow.startNexusOperation(encrypted2AsyncService::echoAsync, message);

    return new EndpointResults(
        compressedEncrypted1Sync.getResult().get(),
        encrypted1Sync.getResult().get(),
        compressedEncrypted2Sync.getResult().get(),
        encrypted2Sync.getResult().get(),
        compressedEncrypted1Async.getResult().get(),
        encrypted1Async.getResult().get(),
        compressedEncrypted2Async.getResult().get(),
        encrypted2Async.getResult().get());
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
