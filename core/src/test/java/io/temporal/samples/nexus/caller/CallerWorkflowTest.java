package io.temporal.samples.nexus.caller;

import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.samples.nexus.handler.HelloHandlerWorkflowImpl;
import io.temporal.samples.nexus.handler.SampleNexusServiceImpl;
import io.temporal.samples.nexus.service.SampleNexusService;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;

// This is an example of how to unit test Nexus services in JUnit4. The handlers are not mocked,
// but are actually called by the testing framework by the caller classes.

public class CallerWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          // If a Nexus service is registered as part of the test as in the following line of code,
          // the TestWorkflowRule will, by default, automatically create a Nexus service endpoint
          // and workflows registered as part of the TestWorkflowRule
          // will automatically inherit the endpoint if none is set.
          .setNexusServiceImplementation(new SampleNexusServiceImpl())
          // The Echo Nexus handler service just makes a call to a class, so no extra setup is
          // needed. But the Hello Nexus service needs a worker for both the caller and handler
          // in order to run.
          // setWorkflowTypes will take the classes given and create workers for them, enabling
          // workflows to run. This is not adding an EchoCallerWorkflow though -
          // see the testEchoWorkflow test method below for an example of an alternate way
          // to supply a worker that gives you more flexibility if needed.
          .setWorkflowTypes(HelloCallerWorkflowImpl.class, HelloHandlerWorkflowImpl.class)
          // Disable automatic worker startup as we are going to register some workflows manually
          // per test
          .setDoNotStart(true)
          .build();

  @Test
  public void testHelloWorkflow() {
    testWorkflowRule.getTestEnvironment().start();

    HelloCallerWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloCallerWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    String greeting = workflow.hello("World", SampleNexusService.Language.EN);
    assertEquals("Hello World 👋", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test
  public void testEchoWorkflow() {
    // If Workflows are registered later than the endpoint can be set manually
    // either by setting the endpoint in the NexusServiceOptions in the Workflow implementation or
    // by setting the NexusServiceOptions on the WorkflowImplementationOptions when registering
    // the Workflow. To demonstrate, this is creating the Nexus service for Echo,
    // and registering a EchoCallerWorkflowImpl worker.
    //
    // It is much simpler to use the setWorkflowTypes in the rule definition above - and as
    // this isn't easily do-able in JUnit5 (the nexus endpoint isn't exposed) should be
    // used with caution.
    testWorkflowRule
        .getWorker()
        .registerWorkflowImplementationTypes(
            WorkflowImplementationOptions.newBuilder()
                .setNexusServiceOptions(
                    Collections.singletonMap(
                        "SampleNexusService",
                        NexusServiceOptions.newBuilder()
                            .setEndpoint(testWorkflowRule.getNexusEndpoint().getSpec().getName())
                            .build()))
                .build(),
            EchoCallerWorkflowImpl.class);
    testWorkflowRule.getTestEnvironment().start();

    EchoCallerWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                EchoCallerWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    String greeting = workflow.echo("Hello");
    assertEquals("Hello", greeting);
    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
