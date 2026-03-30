package io.temporal.samples.nexuscancellation.handler;

import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.samples.nexus.handler.HelloHandlerWorkflow;
import io.temporal.samples.nexus.service.SampleNexusService;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;

public class HelloHandlerWorkflowImpl implements HelloHandlerWorkflow {
  public static final Logger log = Workflow.getLogger(HelloHandlerWorkflowImpl.class);

  @Override
  public SampleNexusService.HelloOutput hello(SampleNexusService.HelloInput input) {
    // Sleep for a random duration to simulate some work
    try {
      Workflow.sleep(Duration.ofSeconds(Workflow.newRandom().nextInt(5)));
      switch (input.getLanguage()) {
        case EN:
          return new SampleNexusService.HelloOutput("Hello " + input.getName() + " 👋");
        case FR:
          return new SampleNexusService.HelloOutput("Bonjour " + input.getName() + " 👋");
        case DE:
          return new SampleNexusService.HelloOutput("Hallo " + input.getName() + " 👋");
        case ES:
          return new SampleNexusService.HelloOutput("¡Hola! " + input.getName() + " 👋");
        case TR:
          return new SampleNexusService.HelloOutput("Merhaba " + input.getName() + " 👋");
      }
      throw ApplicationFailure.newFailure(
          "Unsupported language: " + input.getLanguage(), "UNSUPPORTED_LANGUAGE");
    } catch (CanceledFailure e) {
      // Simulate some work after cancellation is requested
      Workflow.newDetachedCancellationScope(
              () -> Workflow.sleep(Duration.ofSeconds(Workflow.newRandom().nextInt(5))))
          .run();
      log.info("HelloHandlerWorkflow was cancelled successfully.");
      throw e;
    }
  }
}
