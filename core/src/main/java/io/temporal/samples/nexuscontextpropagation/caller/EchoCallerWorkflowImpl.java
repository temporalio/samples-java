package io.temporal.samples.nexuscontextpropagation.caller;

import io.temporal.samples.nexus.caller.EchoCallerWorkflow;
import io.temporal.samples.nexus.service.NexusService;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.MDC;

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
    MDC.put("x-nexus-caller-workflow-id", Workflow.getInfo().getWorkflowId());
    return nexusService.echo(new NexusService.EchoInput(message)).getMessage();
  }
}
