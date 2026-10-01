package io.temporal.samples.nexusserializationcontext.caller;

public record EndpointResults(
    String compressedEncryptedSyncResult,
    String encryptedSyncResult,
    String compressedEncryptedAsyncResult,
    String encryptedAsyncResult) {}
