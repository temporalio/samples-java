package io.temporal.samples.nexusmultipleargs.handler;

import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.nexus.service.SampleNexusService;

public class HelloHandlerWorkflowImpl implements HelloHandlerWorkflow {
  @Override
  public SampleNexusService.HelloOutput hello(String name, SampleNexusService.Language language) {
    switch (language) {
      case EN:
        return new SampleNexusService.HelloOutput("Hello " + name + " 👋");
      case FR:
        return new SampleNexusService.HelloOutput("Bonjour " + name + " 👋");
      case DE:
        return new SampleNexusService.HelloOutput("Hallo " + name + " 👋");
      case ES:
        return new SampleNexusService.HelloOutput("¡Hola! " + name + " 👋");
      case TR:
        return new SampleNexusService.HelloOutput("Merhaba " + name + " 👋");
    }
    throw ApplicationFailure.newFailure(
        "Unsupported language: " + language, "UNSUPPORTED_LANGUAGE");
  }
}
