

package io.temporal.samples.nexus.caller;

import io.temporal.samples.nexus.service.NexusService;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class EchoCallerWorkflowImpl implements EchoCallerWorkflow {
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
  public String echo(String message) {
    return nexusService.echo(new NexusService.EchoInput(message)).getMessage();
  }
}
