

package io.temporal.samples.nexus.handler;

import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.nexus.service.NexusService;

public class HelloHandlerWorkflowImpl implements HelloHandlerWorkflow {
  @Override
  public NexusService.HelloOutput hello(NexusService.HelloInput input) {
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
