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
  public EndpointResults echoThroughBothEndpoints(String message) {
    EchoService compressedEncryptedService = serviceFor(SampleConfig.COMPRESSED_ENCRYPTED_ENDPOINT);
    EchoService encryptedService = serviceFor(SampleConfig.ENCRYPTED_ENDPOINT);

    // Start both before awaiting either result. Each result must keep its own endpoint context.
    NexusOperationHandle<String> compressedEncryptedOperation =
        Workflow.startNexusOperation(compressedEncryptedService::echo, message);
    NexusOperationHandle<String> encryptedOperation =
        Workflow.startNexusOperation(encryptedService::echo, message);
    return new EndpointResults(
        compressedEncryptedOperation.getResult().get(), encryptedOperation.getResult().get());
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
