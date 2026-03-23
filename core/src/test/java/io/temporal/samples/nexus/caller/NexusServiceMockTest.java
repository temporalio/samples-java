package io.temporal.samples.nexus.caller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.nexusrpc.handler.OperationHandler;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.nexus.handler.NexusServiceImpl;
import io.temporal.samples.nexus.service.NexusService;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

// This unit test example shows how to mock the Nexus service itself in JUnit4.
// In this example since the NexusService itself is mocked, no handlers need to be set up or mocked.

public class NexusServiceMockTest {

  // Stubs must be set up before TestWorkflowRule initializes, because ServiceImplInstance calls
  // each @OperationImpl method on the mock during TestWorkflowRule.init() to capture the
  // OperationHandler objects. Any stub set up after that point has no effect.
  // JUnit 4 creates a new test class instance per test method, so this mock is fresh each time.
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

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          // If a Nexus service is registered as part of the test as in the following line of code,
          // the TestWorkflowRule will, by default, automatically create a Nexus service endpoint
          // and workflows registered as part of the TestWorkflowRule
          // will automatically inherit the endpoint if none is set.
          .setNexusServiceImplementation(mockNexusService)
          // The Echo Nexus handler service just makes a call to a class, so no extra setup is
          // needed. But the Hello Nexus service needs a worker for both the caller and handler
          // in order to run.
          // setWorkflowTypes will take the classes given and create workers for them, enabling
          // workflows to run. This creates caller workflows, the handler workflows
          // will be mocked in the test methods.
          .setWorkflowTypes(HelloCallerWorkflowImpl.class, EchoCallerWorkflowImpl.class)
          // Disable automatic worker startup as we are going to register some workflows manually
          // per test
          .setDoNotStart(true)
          .build();

  @Test
  public void testHelloWorkflow() {
    // Set the mock value to return
    helloResult = new NexusService.HelloOutput("Hello Mock World 👋");
    testWorkflowRule.getTestEnvironment().start();

    HelloCallerWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloCallerWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    String greeting = workflow.hello("World", NexusService.Language.EN);
    assertEquals("Hello Mock World 👋", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test
  public void testEchoWorkflow() {
    // Set the mock value to return
    echoResult = new NexusService.EchoOutput("mocked echo");
    testWorkflowRule.getTestEnvironment().start();

    EchoCallerWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                EchoCallerWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    String greeting = workflow.echo("Hello");
    assertEquals("mocked echo", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
