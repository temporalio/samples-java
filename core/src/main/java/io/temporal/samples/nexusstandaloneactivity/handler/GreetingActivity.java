package io.temporal.samples.nexusstandaloneactivity.handler;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.samples.nexusstandaloneactivity.service.GreetingNexusService;

/** Activity used as the backing execution for the Nexus operation. */
@ActivityInterface
public interface GreetingActivity {

  @ActivityMethod
  GreetingNexusService.GreetingOutput createGreeting(GreetingNexusService.GreetingInput input);
}
