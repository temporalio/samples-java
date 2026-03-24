package io.temporal.samples.nexus.caller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.samples.nexus.service.SampleNexusService;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

// This unit test example shows how to mock the Nexus service itself in JUnit4.
// This is the path to take when you don't have access to the service implementation so
// cannot mock it. Since the SampleNexusService itself is mocked,
// no handlers need to be set up or mocked.
public class NexusServiceJunit5Test {

  private final SampleNexusService mockNexusService = mock(SampleNexusService.class);

  /**
   * A test-only Nexus service implementation that delegates to the Mockito mock defined above. Both
   * operations are implemented as synchronous handlers that forward calls to the mock, allowing
   * full control over return values and verification of inputs.
   */
  @ServiceImpl(service = SampleNexusService.class)
  public class TestNexusServiceImpl {
    @OperationImpl
    public OperationHandler<SampleNexusService.EchoInput, SampleNexusService.EchoOutput> echo() {
      return OperationHandler.sync((ctx, details, input) -> mockNexusService.echo(input));
    }

    @OperationImpl
    public OperationHandler<SampleNexusService.HelloInput, SampleNexusService.HelloOutput> hello() {
      return OperationHandler.sync((ctx, details, input) -> mockNexusService.hello(input));
    }
  }

  // Using OperationHandler.sync for both operations bypasses the need for a backing workflow,
  // returning results inline just like a synchronous call.
  //
  // Note that the Mocks need to be done before the extension
  // is defined, as creating the rule will fail if either call is still null.

  @RegisterExtension
  public final TestWorkflowExtension testWorkflowExtension =
      TestWorkflowExtension.newBuilder()
          // If a Nexus service is registered as part of the test as in the following line of code,
          // the TestWorkflowExtension will, by default, automatically create a Nexus service
          // endpoint and workflows registered as part of the TestWorkflowExtension will
          // automatically inherit the endpoint if none is set.
          .setNexusServiceImplementation(new TestNexusServiceImpl())
          // The Echo Nexus handler service just makes a call to a class, so no extra setup is
          // needed. But the Hello Nexus service needs a worker for both the caller and handler
          // in order to run, and the Echo Nexus caller service needs a worker.
          //
          // registerWorkflowImplementationTypes will take the classes given and create workers for
          // them, enabling workflows to run.
          // Since both operations are mocked with OperationHandler.sync, no backing workflow is
          // needed for hello — only the caller workflow types need to be registered.
          .registerWorkflowImplementationTypes(
              HelloCallerWorkflowImpl.class, EchoCallerWorkflowImpl.class)
          // The workflow will start before each test, and will shut down after each test.
          // See CallerWorkflowTest for an example of how to control this differently if needed.
          .build();

  // The TestWorkflowExtension extension in the Temporal testing library creates the
  // arguments to the test cases and initializes them from the extension setup call above.
  @Test
  public void testHelloWorkflow(
      TestWorkflowEnvironment testEnv, Worker worker, HelloCallerWorkflow workflow) {

    // Set the mock value to return
    when(mockNexusService.hello(any()))
        .thenReturn(new SampleNexusService.HelloOutput("Hello Mock World 👋"));

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.hello("World", SampleNexusService.Language.EN);
    assertEquals("Hello Mock World 👋", greeting);
  }

  @Test
  public void testEchoWorkflow(
      TestWorkflowEnvironment testEnv, Worker worker, EchoCallerWorkflow workflow) {
    when(mockNexusService.echo(any()))
        .thenReturn(new SampleNexusService.EchoOutput("echo response"));

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.echo("echo input");
    assertEquals("echo response", greeting);

    // Verify the echo operation was called exactly once and no other operations were invoked
    verify(mockNexusService, times(1)).echo(any());
    // Verify the Nexus service was called with the correct input
    verify(mockNexusService).echo(argThat(input -> "echo input".equals(input.getMessage())));

    verifyNoMoreInteractions(mockNexusService);
  }
}
