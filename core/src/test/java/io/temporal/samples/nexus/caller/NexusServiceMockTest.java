package io.temporal.samples.nexus.caller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.nexus.service.SampleNexusService;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

// This unit test example shows how to mock the Nexus service itself in JUnit4.
// This is the path to take when you don't have access to the service implementation so
// cannot mock it. Since the SampleNexusService itself is mocked,
// no handlers need to be set up or mocked.
public class NexusServiceMockTest {

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
  // Note that the Mocks need to be done before the rule
  // is defined, as creating the rule will fail if either call is still null.

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setNexusServiceImplementation(new TestNexusServiceImpl())
          .setWorkflowTypes(EchoCallerWorkflowImpl.class, HelloCallerWorkflowImpl.class)
          .build();

  @Test
  public void testHelloCallerWithMockedService() {
    when(mockNexusService.hello(any()))
        .thenReturn(new SampleNexusService.HelloOutput("Bonjour World"));

    HelloCallerWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloCallerWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    String result = workflow.hello("World", SampleNexusService.Language.FR);
    assertEquals("Bonjour World", result);

    // Verify the Nexus service was called with the correct name and language
    verify(mockNexusService)
        .hello(
            argThat(
                input ->
                    "World".equals(input.getName())
                        && SampleNexusService.Language.FR == input.getLanguage()));
  }

  @Test
  public void testEchoCallerWithMockedService() {
    when(mockNexusService.echo(any()))
        .thenReturn(new SampleNexusService.EchoOutput("echo response"));

    EchoCallerWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                EchoCallerWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    String echoOutput = workflow.echo("echo input");

    assertEquals("echo response", echoOutput);

    // Verify the echo operation was called exactly once and no other operations were invoked
    verify(mockNexusService, times(1)).echo(any());
    // Verify the Nexus service was called with the correct input
    verify(mockNexusService).echo(argThat(input -> "echo input".equals(input.getMessage())));

    verifyNoMoreInteractions(mockNexusService);
  }
}
