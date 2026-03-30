package io.temporal.samples.nexus.handler;

import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.nexus.service.SampleNexusService;

public class HelloHandlerWorkflowImpl implements HelloHandlerWorkflow {
  @Override
  public SampleNexusService.HelloOutput hello(SampleNexusService.HelloInput input) {
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
  }
}
