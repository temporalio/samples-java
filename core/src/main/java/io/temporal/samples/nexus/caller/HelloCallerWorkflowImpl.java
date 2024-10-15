package io.temporal.samples.nexus.caller;

import io.temporal.samples.nexus.service.NexusService;
import io.temporal.workflow.NexusOperationHandle;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class HelloCallerWorkflowImpl implements HelloCallerWorkflow {
  NexusService nexusService =
      Workflow.newNexusServiceStub(
          NexusService.class,
          NexusServiceOptions.newBuilder()
              .setOperationOptions(
                  NexusOperationOptions.newBuilder()
                      .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                      .build())
              .build());

  @Override
  public String hello(String message, NexusService.Language language) {
    NexusOperationHandle<NexusService.HelloOutput> handle =
        Workflow.startNexusOperation(
            nexusService::hello, new NexusService.HelloInput(message, language));
    // Optionally wait for the operation to be started. NexusOperationExecution will contain the
    // operation ID in case this operation is asynchronous.
    handle.getExecution().get();
    return handle.getResult().get().getMessage();
  }
}
