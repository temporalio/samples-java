package io.temporal.samples.nexussecurecredentialpropagation.caller;

import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * Calls the protected Nexus Operation. The Workflow never handles the credential: the caller Worker
 * interceptor attaches it.
 */
public class TransferWorkflowImpl implements TransferWorkflow {
  private final TransferService transferService =
      Workflow.newNexusServiceStub(
          TransferService.class,
          NexusServiceOptions.newBuilder()
              .setOperationOptions(
                  NexusOperationOptions.newBuilder()
                      .setScheduleToCloseTimeout(Duration.ofMinutes(5))
                      .build())
              .build());

  @Override
  public TransferService.TransferOutput transfer(TransferService.TransferInput input) {
    return transferService.transfer(input);
  }
}
