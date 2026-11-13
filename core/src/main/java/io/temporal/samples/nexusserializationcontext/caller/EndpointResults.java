package io.temporal.samples.nexusserializationcontext.caller;

public record EndpointResults(
    String handlerA1SyncResult,
    String handlerB1SyncResult,
    String handlerA2SyncResult,
    String handlerB2SyncResult,
    String handlerA1AsyncResult,
    String handlerB1AsyncResult,
    String handlerA2AsyncResult,
    String handlerB2AsyncResult) {}
