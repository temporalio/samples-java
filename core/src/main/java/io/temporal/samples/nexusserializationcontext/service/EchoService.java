package io.temporal.samples.nexusserializationcontext.service;

import io.nexusrpc.Operation;
import io.nexusrpc.Service;

@Service(name = EchoService.SERVICE_NAME)
public interface EchoService {
  String SERVICE_NAME = "EchoService";
  String ECHO_OPERATION_NAME = "echo";
  String ECHO_ASYNC_OPERATION_NAME = "echoAsync";

  @Operation(name = ECHO_OPERATION_NAME)
  String echo(String message);

  @Operation(name = ECHO_ASYNC_OPERATION_NAME)
  String echoAsync(String message);
}
