package io.temporal.samples.nexus.caller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.nexus.handler.EchoClient;
import io.temporal.samples.nexus.handler.HelloHandlerWorkflow;
import io.temporal.samples.nexus.handler.SampleNexusServiceImpl;
import io.temporal.samples.nexus.service.SampleNexusService;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

// This is an example of how to unit test Nexus services in JUnit4. The handlers are mocked,
// so that the caller classes interact with the mocks and not the handler classes themselves.

// @@@SNIPSTART java-nexus-sample-junit4-mock
public class CallerWorkflowMockTest {

  // Inject a mock EchoClient so sync Nexus operations can be stubbed per test.
  // JUnit 4 creates a new test class instance per test method, so this mock is fresh each time.
  private final EchoClient mockEchoClient = mock(EchoClient.class);

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          // If a Nexus service is registered as part of the test as in the following line of code,
          // the TestWorkflowRule will, by default, automatically create a Nexus service endpoint
          // and workflows registered as part of the TestWorkflowRule
          // will automatically inherit the endpoint if none is set.
          .setNexusServiceImplementation(new SampleNexusServiceImpl(mockEchoClient))
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
    testWorkflowRule
        .getWorker()
        // Workflows started by a Nexus service can be mocked just like any other workflow
        .registerWorkflowImplementationFactory(
            HelloHandlerWorkflow.class,
            () -> {
              HelloHandlerWorkflow wf = mock(HelloHandlerWorkflow.class);
              when(wf.hello(any()))
                  .thenReturn(new SampleNexusService.HelloOutput("Hello Mock World 👋"));
              return wf;
            });
    testWorkflowRule.getTestEnvironment().start();

    // Now create the caller workflow
    HelloCallerWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloCallerWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    String greeting = workflow.hello("World", SampleNexusService.Language.EN);
    assertEquals("Hello Mock World 👋", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test
  public void testEchoWorkflow() {
    // Sync Nexus operations run inline in the handler thread — there is no backing workflow to
    // register a factory for. Instead, stub the injected EchoCient dependency directly.
    when(mockEchoClient.echo(any())).thenReturn(new SampleNexusService.EchoOutput("mocked echo"));
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

// @@@SNIPEND
