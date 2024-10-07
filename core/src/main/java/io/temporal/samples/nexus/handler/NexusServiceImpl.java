package io.temporal.samples.nexus.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.WorkflowClientOperationHandlers;
import io.temporal.samples.nexus.service.NexusService;

@ServiceImpl(service = NexusService.class)
public class NexusServiceImpl {
  @OperationImpl
  public OperationHandler<NexusService.EchoInput, NexusService.EchoOutput> echo() {
    // WorkflowClientOperationHandlers.sync is a meant for exposing simple RPC handlers.
    return WorkflowClientOperationHandlers.sync(
        // The method is provided with an SDK client that can be used for arbitrary calls such as
        // signaling, querying,
        // and listing workflows but implementations are free to make arbitrary calls to other
        // services or databases, or
        // perform simple computations such as this one.
        (ctx, details, client, input) -> new NexusService.EchoOutput(input.getMessage()));
  }

  @OperationImpl
  public OperationHandler<NexusService.HelloInput, NexusService.HelloOutput> hello() {
    // Use the WorkflowClientOperationHandlers.fromWorkflowMethod constructor, which is the easiest
    // way to expose a workflow as an operation.
    // See alternatives at TODO.
    return WorkflowClientOperationHandlers.fromWorkflowMethod(
        (ctx, details, client, input) ->
            client.newWorkflowStub(
                    HelloHandlerWorkflow.class,
                    // Workflow IDs should typically be business meaningful IDs and are used to
                    // dedupe workflow starts.
                    // For this example, we're using the request ID allocated by Temporal when the
                    // caller workflow schedules
                    // the operation, this ID is guaranteed to be stable across retries of this
                    // operation.
                    //
                    // Task queue defaults to the task queue this operation is handled on.
                    WorkflowOptions.newBuilder().setWorkflowId(details.getRequestId()).build())
                ::hello);
  }
}
