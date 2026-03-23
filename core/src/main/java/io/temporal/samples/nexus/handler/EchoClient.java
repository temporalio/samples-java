package io.temporal.samples.nexus.handler;

import io.temporal.samples.nexus.service.NexusService;

public interface EchoClient {
  NexusService.EchoOutput echo(NexusService.EchoInput input);
}
