package io.temporal.samples.nexus.caller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.nexusrpc.handler.OperationHandler;
import io.temporal.samples.nexus.handler.NexusServiceImpl;
import io.temporal.samples.nexus.service.NexusService;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

// This unit test example shows how to mock the Nexus service itself in JUnit4.
// In this example since the NexusService itself is mocked, no handlers need to be set up or mocked.

public class NexusServiceJunit5Test {

  private final NexusServiceImpl mockNexusService = createMockNexusService();

  // Mutable fields — set these in each test method before starting the environment
  // to mock the return values for the Nexus service.
  // Instance fields (not static) so each test gets its own copy; safe for parallel execution.
  private NexusService.EchoOutput echoResult = new NexusService.EchoOutput("default");
  private NexusService.HelloOutput helloResult = new NexusService.HelloOutput("default");

  private NexusServiceImpl createMockNexusService() {
    NexusServiceImpl mock = mock(NexusServiceImpl.class);
    // Using OperationHandler.sync for both operations bypasses the need for a backing workflow,
    // returning results inline just like a synchronous call. Mocks need to be done before the rule
    // is defined, as creating the rule will fail if either call is still null.
    //
    // The following is the simplest - just mock the services to return a value. But then these
    // values cannot change per test case, so this will not always suffice.
    //    when(mock.echo())
    //        .thenReturn(
    //            OperationHandler.sync(
    //                (ctx, details, input) -> new NexusService.EchoOutput("mocked echo")));
    //    when(mock.hello())
    //        .thenReturn(
    //            OperationHandler.sync(
    //                (ctx, details, input) -> new NexusService.HelloOutput("Hello Mock World
    // 👋")));
    //
    // An alternative approach is to create the mocks but set them to return a value that is set in
    // the unit
    // test class above. That allows you to change the return value in each test.
    when(mock.echo()).thenReturn(OperationHandler.sync((ctx, details, input) -> echoResult));
    when(mock.hello()).thenReturn(OperationHandler.sync((ctx, details, input) -> helloResult));

    return mock;
  }

  @RegisterExtension
  public final TestWorkflowExtension testWorkflowExtension =
      TestWorkflowExtension.newBuilder()
          // If a Nexus service is registered as part of the test as in the following line of code,
          // the TestWorkflowExtension will, by default, automatically create a Nexus service
          // endpoint and workflows registered as part of the TestWorkflowExtension will
          // automatically inherit the endpoint if none is set.
          .setNexusServiceImplementation(mockNexusService)
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
    helloResult = new NexusService.HelloOutput("Hello Mock World 👋");
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.hello("World", NexusService.Language.EN);
    assertEquals("Hello Mock World 👋", greeting);
  }

  @Test
  public void testEchoWorkflow(
      TestWorkflowEnvironment testEnv, Worker worker, EchoCallerWorkflow workflow) {
    // Set the mock value to return
    echoResult = new NexusService.EchoOutput("mocked echo");
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.echo("Hello");
    assertEquals("mocked echo", greeting);
  }
}
