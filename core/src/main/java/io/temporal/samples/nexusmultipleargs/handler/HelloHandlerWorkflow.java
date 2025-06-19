package io.temporal.samples.nexusmultipleargs.handler;

import io.temporal.samples.nexus.service.NexusService;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloHandlerWorkflow {
  @WorkflowMethod
  NexusService.HelloOutput hello(String name, NexusService.Language language);
}
