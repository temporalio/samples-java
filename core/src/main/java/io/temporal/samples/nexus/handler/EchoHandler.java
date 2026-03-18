package io.temporal.samples.nexus.handler;

import io.temporal.samples.nexus.service.NexusService;

public interface EchoHandler {
  NexusService.EchoOutput echo(NexusService.EchoInput input);
}
