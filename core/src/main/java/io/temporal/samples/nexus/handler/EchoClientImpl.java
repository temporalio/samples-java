package io.temporal.samples.nexus.handler;

import io.temporal.samples.nexus.service.SampleNexusService;

// Note that this is a class, not a Temporal worker. This is to demonstrate that Nexus services can
// simply call a class instead of a worker for fast operations that don't need retry handling.
public class EchoClientImpl implements EchoClient {
  @Override
  public SampleNexusService.EchoOutput echo(SampleNexusService.EchoInput input) {
    return new SampleNexusService.EchoOutput(input.getMessage());
  }
}
