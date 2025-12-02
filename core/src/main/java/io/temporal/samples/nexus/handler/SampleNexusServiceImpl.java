package io.temporal.samples.nexus.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.Nexus;
import io.temporal.nexus.WorkflowRunOperation;
import io.temporal.samples.nexus.service.SampleNexusService;
import java.util.Locale;

// To create a service implementation, annotate the class with @ServiceImpl and provide the
// interface that the service implements. The service implementation class should have methods that
// return OperationHandler that correspond to the operations defined in the service interface.
@ServiceImpl(service = SampleNexusService.class)
public class SampleNexusServiceImpl {
  private final EchoClient echoClient;

  // The injected EchoClient makes this class unit-testable.
  // The no-arg constructor provides a default; the second allows tests to inject a mock.
  // If you are not using the sync call or do not need to mock a handler, then you will not
  // need this constructor pairing.
  public SampleNexusServiceImpl() {
    this(new EchoClientImpl());
  }

  public SampleNexusServiceImpl(EchoClient echoClient) {
    this.echoClient = echoClient;
  }

  // The Echo Nexus Service exemplifies making a synchronous call using OperationHandler.sync.
  // In this case, it is calling the EchoClient class - not a workflow - and simply returning the
  // result.
  @OperationImpl
  public OperationHandler<SampleNexusService.EchoInput, SampleNexusService.EchoOutput> echo() {
    return OperationHandler.sync(
        // The method is for making arbitrary short calls to other services or databases, or
        // perform simple computations such as this one. Users can also access a workflow client by
        // calling
        // Nexus.getOperationContext().getWorkflowClient(ctx) to make arbitrary calls such as
        // signaling, querying, or listing workflows.
        (ctx, details, input) -> echoClient.echo(input));
  }

  @OperationImpl
  public OperationHandler<SampleNexusService.HelloInput, SampleNexusService.HelloOutput> hello() {
    // Use the WorkflowRunOperation.fromWorkflowMethod constructor, which is the easiest
    // way to expose a workflow as an operation. To expose a workflow with a different input
    // parameters then the operation or from an untyped stub, use the
    // WorkflowRunOperation.fromWorkflowHandler constructor and the appropriate constructor method
    // on WorkflowHandle.
    return WorkflowRunOperation.fromWorkflowMethod(
        (ctx, details, input) ->
            Nexus.getOperationContext()
                    .getWorkflowClient()
                    .newWorkflowStub(
                        HelloHandlerWorkflow.class,
                        // Workflow IDs should typically be business meaningful IDs and are used to
                        // dedupe workflow starts.
                        // For this example, tie the workflow ID to the customer being greeted so
                        // that
                        // repeated operations for the same customer run on the same workflow.
                        //
                        // Task queue defaults to the task queue this operation is handled on.
                        WorkflowOptions.newBuilder()
                            .setWorkflowId(
                                String.format(
                                    "hello-%s-%s",
                                    input.getName(),
                                    input.getLanguage().name().toLowerCase(Locale.ROOT)))
                            .build())
                ::hello);
  }
}
