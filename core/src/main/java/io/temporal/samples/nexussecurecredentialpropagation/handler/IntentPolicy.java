package io.temporal.samples.nexussecurecredentialpropagation.handler;

import io.temporal.samples.nexussecurecredentialpropagation.common.CredentialIntent;
import io.temporal.samples.nexussecurecredentialpropagation.common.EndUserPrincipal;

/**
 * Authorizes the typed input of one protected operation against the verified principal and the
 * client-sealed intent. Implementations compare typed fields; they never canonicalize the input.
 */
public interface IntentPolicy<T> {
  Class<T> inputType();

  /**
   * Authorizes {@code input} for {@code principal} within {@code grant}.
   *
   * @throws AuthorizationDeniedException if the principal may not perform this input, or the input
   *     falls outside the intent
   */
  void authorize(EndUserPrincipal principal, CredentialIntent.RouteGrant grant, T input)
      throws AuthorizationDeniedException;

  /** Thrown when a verified principal is not authorized. */
  final class AuthorizationDeniedException extends Exception {
    public AuthorizationDeniedException(String message) {
      super(message);
    }
  }
}
