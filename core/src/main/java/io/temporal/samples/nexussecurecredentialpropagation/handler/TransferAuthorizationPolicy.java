package io.temporal.samples.nexussecurecredentialpropagation.handler;

import io.temporal.samples.nexussecurecredentialpropagation.common.EndUserPrincipal;
import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;

/** Authorizes a transfer against the verified token. */
public final class TransferAuthorizationPolicy
    implements AuthorizationPolicy<TransferService.TransferInput> {
  @Override
  public Class<TransferService.TransferInput> inputType() {
    return TransferService.TransferInput.class;
  }

  @Override
  public void authorize(EndUserPrincipal principal, TransferService.TransferInput input)
      throws AuthorizationDeniedException {
    if (!principal.getScopes().contains(TransferService.REQUIRED_SCOPE)) {
      throw new AuthorizationDeniedException("Missing required scope");
    }
    // The token subject is authoritative; the input's user and tenant are only assertions.
    if (!principal.getTenant().equals(input.getTenantId())) {
      throw new AuthorizationDeniedException("Tenant mismatch");
    }
    if (!principal.getSubject().equals(input.getUserId())) {
      throw new AuthorizationDeniedException("Subject mismatch");
    }
    if (input.getAmountCents() <= 0) {
      throw new AuthorizationDeniedException("Invalid amount");
    }
  }
}
