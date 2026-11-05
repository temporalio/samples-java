package io.temporal.samples.nexussecurecredentialpropagation.handler;

import io.temporal.samples.nexussecurecredentialpropagation.common.EndUserPrincipal;

/** The verified principal of the Nexus request being handled on the current thread. */
public final class RequestPrincipal {
  private static final ThreadLocal<EndUserPrincipal> CURRENT = new ThreadLocal<>();

  private RequestPrincipal() {}

  public static EndUserPrincipal current() {
    EndUserPrincipal principal = CURRENT.get();
    if (principal == null) {
      throw new IllegalStateException("No verified principal for this request");
    }
    return principal;
  }

  static void set(EndUserPrincipal principal) {
    CURRENT.set(principal);
  }

  static void clear() {
    CURRENT.remove();
  }
}
