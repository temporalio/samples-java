package io.temporal.samples.nexus.caller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.samples.nexus.handler.HelloHandlerWorkflowImpl;
import io.temporal.samples.nexus.handler.NexusServiceImpl;
import io.temporal.samples.nexus.service.NexusService;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CallerWorkflowJunit5Test {

  @RegisterExtension
  public static final TestWorkflowExtension testWorkflowExtension =
      TestWorkflowExtension.newBuilder()
          // If a Nexus service is registered as part of the test as in the following line of code,
          // the TestWorkflowExtension will, by default, automatically create a Nexus service
          // endpoint and workflows registered as part of the TestWorkflowExtension will
          // automatically inherit the endpoint if none is set.
          .setNexusServiceImplementation(new NexusServiceImpl())
          // The Echo Nexus handler service just makes a call to a class, so no extra setup is
          // needed. But the Hello Nexus service needs a worker for both the caller and handler
          // in order to run, and the Echo Nexus caller service needs a worker.
          //
          // registerWorkflowImplementationTypes will take the classes given and create workers for
          // them, enabling workflows to run.
          .registerWorkflowImplementationTypes(
              HelloCallerWorkflowImpl.class,
              HelloHandlerWorkflowImpl.class,
              EchoCallerWorkflowImpl.class)
          // The workflow will start before each test, and will shut down after each test.
          // See CallerWorkflowTest for an example of how to control this differently if needed.
          .build();

  // The TestWorkflowExtension extension in the Temporal testing library creates the
  // arguments to the test cases and initializes them from the extension setup call above.
  @Test
  public void testHelloWorkflow(
      TestWorkflowEnvironment testEnv, Worker worker, HelloCallerWorkflow workflow) {
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.hello("World", NexusService.Language.EN);
    assertEquals("Hello World 👋", greeting);
  }

  @Test
  public void testEchoWorkflow(
      TestWorkflowEnvironment testEnv, Worker worker, EchoCallerWorkflow workflow) {
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.echo("Hello");
    assertEquals("Hello", greeting);
  }
}
