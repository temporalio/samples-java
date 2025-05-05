

package io.temporal.samples.nexuscancellation.handler;

import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.nexus.handler.HelloHandlerWorkflow;
import io.temporal.samples.nexus.service.NexusService;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class HelloHandlerWorkflowImpl implements HelloHandlerWorkflow {
  @Override
  public NexusService.HelloOutput hello(NexusService.HelloInput input) {
    // Sleep for a random duration to simulate some work
    Workflow.sleep(Duration.ofSeconds(Workflow.newRandom().nextInt(5)));
    switch (input.getLanguage()) {
      case EN:
        return new NexusService.HelloOutput("Hello " + input.getName() + " 👋");
      case FR:
        return new NexusService.HelloOutput("Bonjour " + input.getName() + " 👋");
      case DE:
        return new NexusService.HelloOutput("Hallo " + input.getName() + " 👋");
      case ES:
        return new NexusService.HelloOutput("¡Hola! " + input.getName() + " 👋");
      case TR:
        return new NexusService.HelloOutput("Merhaba " + input.getName() + " 👋");
    }
    throw ApplicationFailure.newFailure(
        "Unsupported language: " + input.getLanguage(), "UNSUPPORTED_LANGUAGE");
  }
}
