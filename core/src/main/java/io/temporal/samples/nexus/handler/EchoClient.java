package io.temporal.samples.nexus.handler;

import io.temporal.samples.nexus.service.SampleNexusService;

public interface EchoClient {
  SampleNexusService.EchoOutput echo(SampleNexusService.EchoInput input);
}
