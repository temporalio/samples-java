package io.temporal.samples.nexussecurecredentialpropagation.handler;

import io.temporal.samples.nexussecurecredentialpropagation.common.CredentialIntent;
import io.temporal.samples.nexussecurecredentialpropagation.common.EndUserPrincipal;
import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;
import java.util.Map;

/** Authorizes a transfer against the token and the intent the user approved. */
public final class TransferIntentPolicy implements IntentPolicy<TransferService.TransferInput> {
  @Override
  public Class<TransferService.TransferInput> inputType() {
    return TransferService.TransferInput.class;
  }

  @Override
  public void authorize(
      EndUserPrincipal principal,
      CredentialIntent.RouteGrant grant,
      TransferService.TransferInput input)
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

    Map<String, String> constraints = grant.getConstraints();
    if (!input.getPaymentId().equals(constraints.get(TransferService.CONSTRAINT_PAYMENT_ID))) {
      throw new AuthorizationDeniedException("Payment outside intent");
    }
    if (!input.getBeneficiary().equals(constraints.get(TransferService.CONSTRAINT_BENEFICIARY))) {
      throw new AuthorizationDeniedException("Beneficiary outside intent");
    }
    String max = constraints.get(TransferService.CONSTRAINT_MAX_AMOUNT_CENTS);
    long maxAmountCents;
    try {
      maxAmountCents = Long.parseLong(max);
    } catch (NumberFormatException e) {
      throw new AuthorizationDeniedException("Invalid amount constraint");
    }
    if (input.getAmountCents() <= 0 || input.getAmountCents() > maxAmountCents) {
      throw new AuthorizationDeniedException("Amount outside intent");
    }
  }
}
