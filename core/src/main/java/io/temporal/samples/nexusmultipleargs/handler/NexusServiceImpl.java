package io.temporal.samples.nexusmultipleargs.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.Nexus;
import io.temporal.nexus.WorkflowHandle;
import io.temporal.nexus.WorkflowRunOperation;
import io.temporal.samples.nexus.service.NexusService;

// @@@SNIPSTART samples-java-nexus-handler-multiargs
// To create a service implementation, annotate the class with @ServiceImpl and provide the
// interface that the service implements. The service implementation class should have methods that
// return OperationHandler that correspond to the operations defined in the service interface.
@ServiceImpl(service = NexusService.class)
public class NexusServiceImpl {
  @OperationImpl
  public OperationHandler<NexusService.EchoInput, NexusService.EchoOutput> echo() {
    // OperationHandler.sync is a meant for exposing simple RPC handlers.
    return OperationHandler.sync(
        // The method is for making arbitrary short calls to other services or databases, or
        // perform simple computations such as this one. Users can also access a workflow client by
        // calling
        // Nexus.getOperationContext().getWorkflowClient(ctx) to make arbitrary calls such as
        // signaling, querying, or listing workflows.
        (ctx, details, input) -> new NexusService.EchoOutput(input.getMessage()));
  }

  @OperationImpl
  public OperationHandler<NexusService.HelloInput, NexusService.HelloOutput> hello() {
    // If the operation input parameters are different from the workflow input parameters,
    // use the WorkflowRunOperation.fromWorkflowHandler constructor and the appropriate constructor
    // method on WorkflowHandle to map the Nexus input to the workflow parameters.
    return WorkflowRunOperation.fromWorkflowHandle(
        (ctx, details, input) ->
            WorkflowHandle.fromWorkflowMethod(
                Nexus.getOperationContext()
                        .getWorkflowClient()
                        .newWorkflowStub(
                            HelloHandlerWorkflow.class,
                            // Workflow IDs should typically be business meaningful IDs and are used
                            // to
                            // dedupe workflow starts.
                            // For this example, we're using the request ID allocated by Temporal
                            // when
                            // the
                            // caller workflow schedules
                            // the operation, this ID is guaranteed to be stable across retries of
                            // this
                            // operation.
                            //
                            // Task queue defaults to the task queue this operation is handled on.
                            WorkflowOptions.newBuilder()
                                .setWorkflowId(details.getRequestId())
                                .build())
                    ::hello,
                input.getName(),
                input.getLanguage()));
  }
}
// @@@SNIPEND
