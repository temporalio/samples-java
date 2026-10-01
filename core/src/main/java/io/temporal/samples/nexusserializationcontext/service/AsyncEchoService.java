package io.temporal.samples.nexusserializationcontext.service;

import io.nexusrpc.Operation;
import io.nexusrpc.Service;

@Service(name = AsyncEchoService.SERVICE_NAME)
public interface AsyncEchoService {
  String SERVICE_NAME = "AsyncEchoService";
  String ECHO_ASYNC_OPERATION_NAME = "echoAsync";

  @Operation(name = ECHO_ASYNC_OPERATION_NAME)
  String echoAsync(String message);
}
