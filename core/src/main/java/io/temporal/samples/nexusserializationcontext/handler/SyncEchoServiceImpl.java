package io.temporal.samples.nexusserializationcontext.handler;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.samples.nexusserializationcontext.service.SyncEchoService;

@ServiceImpl(service = SyncEchoService.class)
public class SyncEchoServiceImpl {
  @OperationImpl
  public OperationHandler<String, String> echo() {
    return OperationHandler.sync((ctx, details, message) -> message);
  }
}
