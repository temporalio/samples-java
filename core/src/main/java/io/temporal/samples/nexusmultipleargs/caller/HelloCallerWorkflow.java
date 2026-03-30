package io.temporal.samples.nexusmultipleargs.caller;

import io.temporal.samples.nexus.service.SampleNexusService;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloCallerWorkflow {
  @WorkflowMethod
  String hello(String message, SampleNexusService.Language language);
}
