package io.temporal.samples.nexussecurecredentialpropagation.caller;

import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface TransferWorkflow {
  @WorkflowMethod
  TransferService.TransferOutput transfer(TransferService.TransferInput input);
}
