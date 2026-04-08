package io.temporal.samples.nexusmessaging.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.Nexus;
import io.temporal.nexus.WorkflowRunOperation;
import io.temporal.samples.nexusmessaging.service.SampleNexusService;

@ServiceImpl(service = SampleNexusService.class)
public class SampleNexusServiceImpl {

  private final String workflowId;

  public SampleNexusServiceImpl(String workflowId) {
    this.workflowId = workflowId;
  }

  private MessageHandlerWorkflow getWorkflowStub() {
    return Nexus.getOperationContext()
        .getWorkflowClient()
        .newWorkflowStub(MessageHandlerWorkflow.class, workflowId);
  }

  @OperationImpl
  public OperationHandler<
          SampleNexusService.RunFromRemoteInput, SampleNexusService.RunFromRemoteOutput>
      runFromRemote() {
    return WorkflowRunOperation.fromWorkflowMethod(
        (ctx, details, input) ->
            Nexus.getOperationContext()
                    .getWorkflowClient()
                    .newWorkflowStub(
                        MessageHandlerRemoteWorkflow.class,
                        WorkflowOptions.newBuilder().setWorkflowId(input.getWorkflowId()).build())
                ::runFromRemote);
  }

  @OperationImpl
  public OperationHandler<
          SampleNexusService.SignalWorkflowInput, SampleNexusService.SignalWorkflowOutput>
      signalWorkflow() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          getWorkflowStub().signalWorkflow(input);
          return new SampleNexusService.SignalWorkflowOutput();
        });
  }

  @OperationImpl
  public OperationHandler<
          SampleNexusService.QueryWorkflowInput, SampleNexusService.QueryWorkflowOutput>
      queryWorkflow() {
    return OperationHandler.sync((ctx, details, input) -> getWorkflowStub().queryWorkflow(input));
  }

  @OperationImpl
  public OperationHandler<
          SampleNexusService.UpdateWorkflowInput, SampleNexusService.UpdateWorkflowOutput>
      updateWorkflow() {
    return OperationHandler.sync((ctx, details, input) -> getWorkflowStub().updateWorkflow(input));
  }

  @OperationImpl
  public OperationHandler<
          SampleNexusService.QueryWorkflowRemoteStartInput, SampleNexusService.QueryWorkflowOutput>
      queryWorkflowRemoteStart() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          MessageHandlerRemoteWorkflow stub =
              Nexus.getOperationContext()
                  .getWorkflowClient()
                  .newWorkflowStub(MessageHandlerRemoteWorkflow.class, input.getWorkflowId());
          return stub.queryWorkflow(new SampleNexusService.QueryWorkflowInput(input.getName()));
        });
  }

  @OperationImpl
  public OperationHandler<
          SampleNexusService.SignalWorkflowRemoteStartInput,
          SampleNexusService.SignalWorkflowOutput>
      signalWorkflowRemoteStart() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          MessageHandlerRemoteWorkflow stub =
              Nexus.getOperationContext()
                  .getWorkflowClient()
                  .newWorkflowStub(MessageHandlerRemoteWorkflow.class, input.getWorkflowId());
          stub.signalWorkflow(new SampleNexusService.SignalWorkflowInput(input.getName()));
          return new SampleNexusService.SignalWorkflowOutput();
        });
  }

  /*

  @OperationImpl
  public OperationHandler<
          SampleNexusService.SignalWorkflowInput, SampleNexusService.SignalWorkflowOutput>
      signalWorkflow() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          MessageHandlerWorkflow stub =
              Nexus.getOperationContext()
                  .getWorkflowClient()
                  .newWorkflowStub(MessageHandlerWorkflow.class, input.getWorkflowId());
          stub.signalWorkflow(input.getName());
          return new SampleNexusService.SignalWorkflowOutput();
        });
  }



  @OperationImpl
  public OperationHandler<
          SampleNexusService.UpdateWorkflowInput, SampleNexusService.UpdateWorkflowOutput>
      updateWorkflow() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          MessageHandlerWorkflow stub =
              Nexus.getOperationContext()
                  .getWorkflowClient()
                  .newWorkflowStub(MessageHandlerWorkflow.class, input.getWorkflowId());
          int result = stub.updateWorkflow(input.getName());
          return new SampleNexusService.UpdateWorkflowOutput(result);
        });
  }
   */
}
