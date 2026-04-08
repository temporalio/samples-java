package io.temporal.samples.nexusmessaging.handler;

import io.temporal.samples.nexusmessaging.service.SampleNexusService;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.UpdateValidatorMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MessageHandlerRemoteWorkflow {

  @WorkflowMethod
  SampleNexusService.RunFromRemoteOutput runFromRemote(SampleNexusService.RunFromRemoteInput input);

  @QueryMethod
  SampleNexusService.QueryWorkflowOutput queryWorkflow(SampleNexusService.QueryWorkflowInput name);

  @SignalMethod
  void signalWorkflow(SampleNexusService.SignalWorkflowInput name);

  @UpdateMethod
  SampleNexusService.UpdateWorkflowOutput updateWorkflow(
      SampleNexusService.UpdateWorkflowInput name);

  @UpdateValidatorMethod(updateName = "updateWorkflow")
  void setLanguageValidator(SampleNexusService.UpdateWorkflowInput name);
}
