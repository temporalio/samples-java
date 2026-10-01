package io.temporal.samples.nexusserializationcontext.service;

import io.nexusrpc.Operation;
import io.nexusrpc.Service;

@Service(name = SyncEchoService.SERVICE_NAME)
public interface SyncEchoService {
  String SERVICE_NAME = "SyncEchoService";
  String ECHO_OPERATION_NAME = "echo";

  @Operation(name = ECHO_OPERATION_NAME)
  String echo(String message);
}
