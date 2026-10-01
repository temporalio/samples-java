package io.temporal.samples.nexusserializationcontext.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.Nexus;
import io.temporal.nexus.WorkflowRunOperation;
import io.temporal.samples.nexusserializationcontext.service.EchoService;

@ServiceImpl(service = EchoService.class)
public class EchoServiceImpl {
  @OperationImpl
  public OperationHandler<String, String> echo() {
    return OperationHandler.sync((ctx, details, message) -> message);
  }

  @OperationImpl
  public OperationHandler<String, String> echoAsync() {
    return WorkflowRunOperation.fromWorkflowMethod(
        (ctx, details, message) ->
            Nexus.getOperationContext()
                    .getWorkflowClient()
                    .newWorkflowStub(
                        EchoWorkflow.class,
                        WorkflowOptions.newBuilder()
                            .setWorkflowId("nexus-echo-" + details.getRequestId())
                            .build())
                ::echo);
  }
}
