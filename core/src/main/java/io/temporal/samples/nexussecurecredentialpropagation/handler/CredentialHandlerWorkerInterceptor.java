package io.temporal.samples.nexussecurecredentialpropagation.handler;

import com.google.crypto.tink.HybridDecrypt;
import io.nexusrpc.OperationException;
import io.nexusrpc.handler.HandlerException;
import io.nexusrpc.handler.OperationContext;
import io.temporal.common.interceptors.NexusOperationInboundCallsInterceptor;
import io.temporal.common.interceptors.NexusOperationInboundCallsInterceptorBase;
import io.temporal.common.interceptors.NexusOperationOutboundCallsInterceptor;
import io.temporal.common.interceptors.WorkerInterceptorBase;
import io.temporal.samples.nexussecurecredentialpropagation.common.ArgsDigest;
import io.temporal.samples.nexussecurecredentialpropagation.common.CredentialEnvelope;
import io.temporal.samples.nexussecurecredentialpropagation.common.CredentialIntent;
import io.temporal.samples.nexussecurecredentialpropagation.common.EndUserPrincipal;
import io.temporal.samples.nexussecurecredentialpropagation.common.SecureCredentialHeaders;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authenticates and authorizes protected Nexus Operations before their handlers run.
 *
 * <p>Route context comes from the actual Nexus request, never from custom headers. The credential
 * header decrypts only with this route and the digest of the exact input ciphertext the payload
 * codec just decoded. Failures return generic messages; details are logged without claims.
 */
public class CredentialHandlerWorkerInterceptor extends WorkerInterceptorBase {
  private static final Logger logger =
      LoggerFactory.getLogger(CredentialHandlerWorkerInterceptor.class);

  private final HybridDecrypt nexusCredentialDecrypt;
  private final JwtVerifier jwtVerifier;
  private final Map<String, IntentPolicy<?>> policies;

  /**
   * @param policies intent policy per protected route, keyed by {@code service/operation}
   */
  public CredentialHandlerWorkerInterceptor(
      HybridDecrypt nexusCredentialDecrypt,
      JwtVerifier jwtVerifier,
      Map<String, IntentPolicy<?>> policies) {
    this.nexusCredentialDecrypt = nexusCredentialDecrypt;
    this.jwtVerifier = jwtVerifier;
    this.policies = Collections.unmodifiableMap(new HashMap<>(policies));
  }

  @Override
  public NexusOperationInboundCallsInterceptor interceptNexusOperation(
      OperationContext context, NexusOperationInboundCallsInterceptor next) {
    return new Inbound(next);
  }

  private final class Inbound extends NexusOperationInboundCallsInterceptorBase {
    private String endpoint;

    Inbound(NexusOperationInboundCallsInterceptor next) {
      super(next);
    }

    @Override
    public void init(NexusOperationOutboundCallsInterceptor outboundCalls) {
      endpoint = outboundCalls.getInfo().getEndpoint();
      super.init(outboundCalls);
    }

    @Override
    public StartOperationOutput startOperation(StartOperationInput input)
        throws OperationException {
      // Take the digest first: the SDK decoded the input immediately before this call.
      ArgsDigest.Received received = ArgsDigest.take();
      OperationContext context = input.getOperationContext();
      String route = context.getService() + "/" + context.getOperation();
      String header = context.getHeaders().get(SecureCredentialHeaders.NEXUS_HEADER);
      IntentPolicy<?> policy = policies.get(route);
      if (policy == null) {
        if (header != null) {
          throw reject(
              HandlerException.ErrorType.BAD_REQUEST,
              route,
              "credential sent to an unprotected route");
        }
        return super.startOperation(input);
      }

      EndUserPrincipal principal = authenticate(input, received, route, header, policy);
      RequestPrincipal.set(principal);
      try {
        return super.startOperation(input);
      } finally {
        RequestPrincipal.clear();
      }
    }

    private EndUserPrincipal authenticate(
        StartOperationInput input,
        ArgsDigest.Received received,
        String route,
        String header,
        IntentPolicy<?> policy) {
      OperationContext context = input.getOperationContext();
      if (endpoint == null || endpoint.isEmpty()) {
        // Servers before 1.30 do not report the endpoint; fail closed instead of weakening the
        // binding.
        throw reject(HandlerException.ErrorType.BAD_REQUEST, route, "endpoint unavailable");
      }
      if (received == null || received.getCount() != 1 || input.getInput() == null) {
        throw reject(HandlerException.ErrorType.BAD_REQUEST, route, "no input to bind to");
      }
      if (header == null) {
        throw reject(HandlerException.ErrorType.UNAUTHENTICATED, route, "missing credential");
      }

      CredentialEnvelope envelope;
      EndUserPrincipal principal;
      try {
        envelope =
            SecureCredentialHeaders.open(
                nexusCredentialDecrypt,
                SecureCredentialHeaders.parseNexusHeaderValue(header),
                SecureCredentialHeaders.nexusContext(
                    endpoint, context.getService(), context.getOperation(), received.getDigest()));
        principal = jwtVerifier.verify(envelope.getCompactJwt());
      } catch (GeneralSecurityException e) {
        throw reject(HandlerException.ErrorType.UNAUTHENTICATED, route, e.getMessage());
      }

      Optional<CredentialIntent.RouteGrant> grant =
          envelope.getIntent().grantFor(endpoint, context.getService(), context.getOperation());
      if (!grant.isPresent()) {
        throw reject(HandlerException.ErrorType.UNAUTHORIZED, route, "route outside intent");
      }
      try {
        authorize(policy, principal, grant.get(), input.getInput());
      } catch (IntentPolicy.AuthorizationDeniedException e) {
        throw reject(HandlerException.ErrorType.UNAUTHORIZED, route, e.getMessage());
      }
      return principal;
    }
  }

  private static <T> void authorize(
      IntentPolicy<T> policy,
      EndUserPrincipal principal,
      CredentialIntent.RouteGrant grant,
      Object input)
      throws IntentPolicy.AuthorizationDeniedException {
    if (!policy.inputType().isInstance(input)) {
      throw new IntentPolicy.AuthorizationDeniedException("Unexpected input type");
    }
    policy.authorize(principal, grant, policy.inputType().cast(input));
  }

  private static HandlerException reject(
      HandlerException.ErrorType type, String route, String reason) {
    logger.warn("Rejected Nexus request for {}: {} ({})", route, type, reason);
    // No cause: it would be serialized into the caller's history.
    return new HandlerException(
        type,
        type.name().toLowerCase(Locale.ROOT),
        (Throwable) null,
        HandlerException.RetryBehavior.NON_RETRYABLE);
  }
}
