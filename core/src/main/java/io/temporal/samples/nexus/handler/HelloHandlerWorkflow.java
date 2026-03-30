package io.temporal.samples.nexus.handler;

import io.temporal.samples.nexus.service.SampleNexusService;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloHandlerWorkflow {
  @WorkflowMethod
  SampleNexusService.HelloOutput hello(SampleNexusService.HelloInput input);
}
