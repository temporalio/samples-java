package io.temporal.samples.nexuscontextpropagation.caller;

import io.temporal.samples.nexus.caller.EchoCallerWorkflow;
import io.temporal.samples.nexus.service.SampleNexusService;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.MDC;

public class EchoCallerWorkflowImpl implements EchoCallerWorkflow {
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
  public String echo(String message) {
    MDC.put("x-nexus-caller-workflow-id", Workflow.getInfo().getWorkflowId());
    return sampleNexusService.echo(new SampleNexusService.EchoInput(message)).getMessage();
  }
}
