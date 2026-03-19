package io.temporal.samples.nexus.caller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.temporal.samples.nexus.handler.EchoHandler;
import io.temporal.samples.nexus.handler.HelloHandlerWorkflow;
import io.temporal.samples.nexus.handler.NexusServiceImpl;
import io.temporal.samples.nexus.service.NexusService;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CallerWorkflowJunit5MockTest {

  // Sync Nexus operations run inline in the handler thread — there is no backing workflow to
  // register a factory for. To mock one, inject a mock dependency into the service implementation.
  private static final EchoHandler mockEchoHandler = mock(EchoHandler.class);

  @RegisterExtension
  public static final TestWorkflowExtension testWorkflowExtension =
      TestWorkflowExtension.newBuilder()
          // If a Nexus service is registered as part of the test as in the following line of code,
          // the TestWorkflowExtension will, by default, automatically create a Nexus service
          // endpoint and workflows registered as part of the TestWorkflowExtension will
          // automatically inherit the endpoint if none is set.
          .setNexusServiceImplementation(new NexusServiceImpl(mockEchoHandler))
          // The Echo Nexus handler service just makes a call to a class, so no extra setup is
          // needed. But the Hello Nexus service needs a worker for both the caller and handler
          // in order to run, and the Echo Nexus caller service needs a worker.
          //
          // registerWorkflowImplementationTypes will take the classes given and create workers for
          // them, enabling workflows to run.
          .registerWorkflowImplementationTypes(
              HelloCallerWorkflowImpl.class, EchoCallerWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testHelloWorkflow(
      TestWorkflowEnvironment testEnv, Worker worker, HelloCallerWorkflow workflow) {
    // Workflows started by a Nexus service can be mocked just like any other workflow
    worker.registerWorkflowImplementationFactory(
        HelloHandlerWorkflow.class,
        () -> {
          HelloHandlerWorkflow mockHandler = mock(HelloHandlerWorkflow.class);
          when(mockHandler.hello(any()))
              .thenReturn(new NexusService.HelloOutput("Hello Mock World 👋"));
          return mockHandler;
        });
    testEnv.start();

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.hello("World", NexusService.Language.EN);
    assertEquals("Hello Mock World 👋", greeting);

    testEnv.shutdown();
  }

  @Test
  public void testEchoWorkflow(
      TestWorkflowEnvironment testEnv, Worker worker, EchoCallerWorkflow workflow) {
    // Sync Nexus operations run inline in the handler thread — there is no backing workflow to
    // register a factory for. Instead, stub the injected EchoHandler dependency directly.
    when(mockEchoHandler.echo(any())).thenReturn(new NexusService.EchoOutput("mocked echo"));
    testEnv.start();

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.echo("Hello");
    assertEquals("mocked echo", greeting);

    testEnv.shutdown();
  }
}
