package io.temporal.samples.nexus.caller;

import io.temporal.samples.nexus.service.NexusService;
import io.temporal.workflow.Workflow;

public class EchoCallerWorkflowImpl implements EchoCallerWorkflow {
  NexusService nexusService = Workflow.newNexusServiceStub(NexusService.class);

  @Override
  public String echo(String message) {
    return nexusService.echo(new NexusService.EchoInput(message)).getMessage();
  }
}
