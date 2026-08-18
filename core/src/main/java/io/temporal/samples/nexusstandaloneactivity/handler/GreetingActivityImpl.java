package io.temporal.samples.nexusstandaloneactivity.handler;

import io.temporal.samples.nexusstandaloneactivity.service.GreetingNexusService;

public class GreetingActivityImpl implements GreetingActivity {

  @Override
  public GreetingNexusService.GreetingOutput createGreeting(
      GreetingNexusService.GreetingInput input) {
    return new GreetingNexusService.GreetingOutput("Hello, " + input.getName() + "!");
  }
}
