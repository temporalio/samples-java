package io.temporal.samples.nexusstandalone.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.Nexus;
import io.temporal.nexus.WorkflowRunOperation;
import io.temporal.samples.nexusstandalone.service.GreetingNexusService;

// Implements the GreetingNexusService operations. startGreeting is backed by a workflow that blocks
// (so it runs long enough to be cancelled/terminated); greet is a synchronous handler that
// completes inline.
@ServiceImpl(service = GreetingNexusService.class)
public class GreetingNexusServiceImpl {

  // Workflow-backed asynchronous operation. WorkflowRunOperation.fromWorkflowMethod exposes a
  // workflow as a Nexus operation: starting the operation starts the workflow, and the operation
  // completes when the workflow returns. The workflow ID is derived deterministically from the
  // input name so the client can address the backing workflow directly (the sample uses this to
  // terminate it by ID — it is just the word "greeting-" plus a known string from the object).
  @OperationImpl
  public OperationHandler<GreetingNexusService.GreetingInput, GreetingNexusService.GreetingOutput>
      startGreeting() {
    return WorkflowRunOperation.fromWorkflowMethod(
        (ctx, details, input) ->
            Nexus.getOperationContext()
                    .getWorkflowClient()
                    .newWorkflowStub(
                        GreetingWorkflow.class,
                        WorkflowOptions.newBuilder()
                            .setWorkflowId("greeting-" + input.getName())
                            .build())
                ::greet);
  }

  // Synchronous operation: OperationHandler.sync runs the lambda inline and returns the result
  // immediately, so the Nexus operation completes as part of the start call.
  @OperationImpl
  public OperationHandler<GreetingNexusService.GreetingInput, GreetingNexusService.GreetingOutput>
      greet() {
    return OperationHandler.sync(
        (ctx, details, input) ->
            new GreetingNexusService.GreetingOutput("Hello, " + input.getName() + "!"));
  }
}
