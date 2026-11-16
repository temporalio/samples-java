package io.temporal.samples.nexusserializationcontext.caller;

public record EndpointResults(
    String compressedEncrypted1SyncResult,
    String encrypted1SyncResult,
    String compressedEncrypted2SyncResult,
    String encrypted2SyncResult,
    String compressedEncrypted1AsyncResult,
    String encrypted1AsyncResult,
    String compressedEncrypted2AsyncResult,
    String encrypted2AsyncResult) {}
