package io.temporal.samples.nexussecurecredentialpropagation.common;

import com.google.crypto.tink.jwt.JwtPublicKeySign;
import com.google.crypto.tink.jwt.RawJwt;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Stands in for a real identity provider. It issues short-lived, audience-restricted access tokens
 * for the handler, as a token exchange at the client edge would.
 */
public final class DemoIdentityProvider {
  public static final String ISSUER = "https://idp.example.com";
  public static final String HANDLER_AUDIENCE = "transfer-service";
  public static final String TOKEN_TYPE = "at+jwt";
  public static final String TENANT_CLAIM = "tenant";
  public static final String SCOPE_CLAIM = "scope";

  private final JwtPublicKeySign signer;

  public DemoIdentityProvider(JwtPublicKeySign signer) {
    this.signer = signer;
  }

  public String issue(String subject, String tenant, String scope, Duration lifetime) {
    return issue(subject, tenant, scope, HANDLER_AUDIENCE, Instant.now(), lifetime);
  }

  public String issue(
      String subject,
      String tenant,
      String scope,
      String audience,
      Instant notBefore,
      Duration lifetime) {
    RawJwt jwt =
        RawJwt.newBuilder()
            .setTypeHeader(TOKEN_TYPE)
            .setIssuer(ISSUER)
            .setAudience(audience)
            .setSubject(subject)
            .setJwtId(UUID.randomUUID().toString())
            .setIssuedAt(Instant.now())
            .setNotBefore(notBefore)
            .setExpiration(notBefore.plus(lifetime))
            .addStringClaim(TENANT_CLAIM, tenant)
            .addStringClaim(SCOPE_CLAIM, scope)
            .build();
    try {
      return signer.signAndEncode(jwt);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
