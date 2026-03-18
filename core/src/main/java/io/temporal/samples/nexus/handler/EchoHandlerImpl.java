package io.temporal.samples.nexus.handler;

import io.temporal.samples.nexus.service.NexusService;

// Note that this is a class, not a Temporal worker. This is to demonstrate that Nexus services can
// simply call a class instead of a worker for fast operations that don't need retry handling.
public class EchoHandlerImpl implements EchoHandler {
  @Override
  public NexusService.EchoOutput echo(NexusService.EchoInput input) {
    return new NexusService.EchoOutput(input.getMessage());
  }
}
