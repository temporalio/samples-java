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
    SyncEchoService handlerA1SyncService =
        serviceFor(SyncEchoService.class, SampleConfig.HANDLER_A_1_ENDPOINT);
    SyncEchoService handlerB1SyncService =
        serviceFor(SyncEchoService.class, SampleConfig.HANDLER_B_1_ENDPOINT);
    SyncEchoService handlerA2SyncService =
        serviceFor(SyncEchoService.class, SampleConfig.HANDLER_A_2_ENDPOINT);
    SyncEchoService handlerB2SyncService =
        serviceFor(SyncEchoService.class, SampleConfig.HANDLER_B_2_ENDPOINT);
    AsyncEchoService handlerA1AsyncService =
        serviceFor(AsyncEchoService.class, SampleConfig.HANDLER_A_1_ENDPOINT);
    AsyncEchoService handlerB1AsyncService =
        serviceFor(AsyncEchoService.class, SampleConfig.HANDLER_B_1_ENDPOINT);
    AsyncEchoService handlerA2AsyncService =
        serviceFor(AsyncEchoService.class, SampleConfig.HANDLER_A_2_ENDPOINT);
    AsyncEchoService handlerB2AsyncService =
        serviceFor(AsyncEchoService.class, SampleConfig.HANDLER_B_2_ENDPOINT);

    // Start all eight operations before awaiting results. Each keeps its endpoint context.
    NexusOperationHandle<String> handlerA1Sync =
        Workflow.startNexusOperation(handlerA1SyncService::echo, message);
    NexusOperationHandle<String> handlerA1Async =
        Workflow.startNexusOperation(handlerA1AsyncService::echoAsync, message);
    NexusOperationHandle<String> handlerB1Sync =
        Workflow.startNexusOperation(handlerB1SyncService::echo, message);
    NexusOperationHandle<String> handlerB1Async =
        Workflow.startNexusOperation(handlerB1AsyncService::echoAsync, message);
    NexusOperationHandle<String> handlerA2Sync =
        Workflow.startNexusOperation(handlerA2SyncService::echo, message);
    NexusOperationHandle<String> handlerA2Async =
        Workflow.startNexusOperation(handlerA2AsyncService::echoAsync, message);
    NexusOperationHandle<String> handlerB2Sync =
        Workflow.startNexusOperation(handlerB2SyncService::echo, message);
    NexusOperationHandle<String> handlerB2Async =
        Workflow.startNexusOperation(handlerB2AsyncService::echoAsync, message);

    return new EndpointResults(
        handlerA1Sync.getResult().get(),
        handlerB1Sync.getResult().get(),
        handlerA2Sync.getResult().get(),
        handlerB2Sync.getResult().get(),
        handlerA1Async.getResult().get(),
        handlerB1Async.getResult().get(),
        handlerA2Async.getResult().get(),
        handlerB2Async.getResult().get());
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
