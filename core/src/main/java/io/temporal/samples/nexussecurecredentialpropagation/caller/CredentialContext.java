package io.temporal.samples.nexussecurecredentialpropagation.caller;

import io.temporal.samples.nexussecurecredentialpropagation.common.CredentialEnvelope;
import java.util.function.Supplier;

/**
 * Short-lived client-side holder for the credential to attach to Workflow starts made on the
 * current thread.
 */
public final class CredentialContext {
  private static final ThreadLocal<CredentialEnvelope> CURRENT = new ThreadLocal<>();

  private CredentialContext() {}

  /** Runs {@code action} with the credential attached to any Workflow start it makes. */
  public static <T> T callWith(CredentialEnvelope credential, Supplier<T> action) {
    if (CURRENT.get() != null) {
      throw new IllegalStateException("A credential is already in scope on this thread");
    }
    CURRENT.set(credential);
    try {
      return action.get();
    } finally {
      CURRENT.remove();
    }
  }

  static CredentialEnvelope current() {
    return CURRENT.get();
  }
}
