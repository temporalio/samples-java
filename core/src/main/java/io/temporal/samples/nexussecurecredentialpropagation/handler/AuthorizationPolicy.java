package io.temporal.samples.nexussecurecredentialpropagation.handler;

import io.temporal.samples.nexussecurecredentialpropagation.common.EndUserPrincipal;

/**
 * Authorizes the typed input of one protected operation against the verified principal.
 * Implementations compare typed fields; they never canonicalize the input.
 */
public interface AuthorizationPolicy<T> {
  Class<T> inputType();

  /**
   * Authorizes {@code input} for {@code principal}.
   *
   * @throws AuthorizationDeniedException if the principal may not perform this input
   */
  void authorize(EndUserPrincipal principal, T input) throws AuthorizationDeniedException;

  /** Thrown when a verified principal is not authorized. */
  final class AuthorizationDeniedException extends Exception {
    public AuthorizationDeniedException(String message) {
      super(message);
    }
  }
}
