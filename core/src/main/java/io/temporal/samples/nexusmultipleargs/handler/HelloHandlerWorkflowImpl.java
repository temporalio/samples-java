package io.temporal.samples.nexusmultipleargs.handler;

import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.nexus.service.NexusService;

public class HelloHandlerWorkflowImpl implements HelloHandlerWorkflow {
  @Override
  public NexusService.HelloOutput hello(String name, NexusService.Language language) {
    switch (language) {
      case EN:
        return new NexusService.HelloOutput("Hello " + name + " 👋");
      case FR:
        return new NexusService.HelloOutput("Bonjour " + name + " 👋");
      case DE:
        return new NexusService.HelloOutput("Hallo " + name + " 👋");
      case ES:
        return new NexusService.HelloOutput("¡Hola! " + name + " 👋");
      case TR:
        return new NexusService.HelloOutput("Merhaba " + name + " 👋");
    }
    throw ApplicationFailure.newFailure(
        "Unsupported language: " + language, "UNSUPPORTED_LANGUAGE");
  }
}
