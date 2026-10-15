package io.temporal.samples.nexussecurecredentialpropagation.handler;

import io.temporal.samples.nexussecurecredentialpropagation.common.SampleKeysets;

/** Exposes the operation token signing of {@link SignedTokenOperationHandler} to tests. */
public final class TokenProbe {
  private final SignedTokenOperationHandler<Object, Object> handler;

  public TokenProbe(SampleKeysets keys) {
    this.handler = new SignedTokenOperationHandler<>(keys.operationTokenMac(), null, null);
  }

  public String sign(String workflowId) {
    return handler.sign(workflowId);
  }

  public String verify(String token) {
    return handler.verify(token);
  }
}
