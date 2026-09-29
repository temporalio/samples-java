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
  public EndpointResults callBoth(String message) {
    EchoService hmacService = serviceFor(SampleConfig.HMAC_ENDPOINT);
    EchoService zlibService = serviceFor(SampleConfig.ZLIB_ENDPOINT);

    // Start both before awaiting either result. Each result must keep its own endpoint context.
    NexusOperationHandle<String> hmacOperation =
        Workflow.startNexusOperation(hmacService::echo, message);
    NexusOperationHandle<String> zlibOperation =
        Workflow.startNexusOperation(zlibService::echo, message);
    return new EndpointResults(hmacOperation.getResult().get(), zlibOperation.getResult().get());
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
