package io.temporal.samples.nexus.caller;

import io.temporal.samples.nexus.service.SampleNexusService;
import io.temporal.workflow.NexusOperationHandle;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class HelloCallerWorkflowImpl implements HelloCallerWorkflow {
  SampleNexusService sampleNexusService =
      Workflow.newNexusServiceStub(
          SampleNexusService.class,
          NexusServiceOptions.newBuilder()
              .setOperationOptions(
                  NexusOperationOptions.newBuilder()
                      .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                      .build())
              .build());

  @Override
  public String hello(String message, SampleNexusService.Language language) {
    NexusOperationHandle<SampleNexusService.HelloOutput> handle =
        Workflow.startNexusOperation(
            sampleNexusService::hello, new SampleNexusService.HelloInput(message, language));
    // Optionally wait for the operation to be started. NexusOperationExecution will contain the
    // operation token in case this operation is asynchronous.
    handle.getExecution().get();
    return handle.getResult().get().getMessage();
  }
}
