package io.temporal.samples.nexussecurecredentialpropagation.handler;

import com.google.crypto.tink.jwt.JwtPublicKeyVerify;
import com.google.crypto.tink.jwt.JwtValidator;
import com.google.crypto.tink.jwt.VerifiedJwt;
import io.temporal.samples.nexussecurecredentialpropagation.common.DemoIdentityProvider;
import io.temporal.samples.nexussecurecredentialpropagation.common.EndUserPrincipal;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates the end user's access token at the handler, the only place it is validated.
 *
 * <p>The algorithm allowlist is the verification keyset itself: Tink accepts only tokens whose
 * {@code alg} header matches a key in the keyset (ES256 here). Tink also enforces {@code exp} and
 * {@code nbf}.
 */
public final class JwtVerifier {
  private final JwtPublicKeyVerify verify;
  private final JwtValidator validator;

  public JwtVerifier(JwtPublicKeyVerify verify, String issuer, String audience) {
    this(verify, issuer, audience, Clock.systemUTC());
  }

  public JwtVerifier(JwtPublicKeyVerify verify, String issuer, String audience, Clock clock) {
    this.verify = verify;
    this.validator =
        JwtValidator.newBuilder()
            .expectTypeHeader(DemoIdentityProvider.TOKEN_TYPE)
            .expectIssuer(issuer)
            .expectAudience(audience)
            .expectIssuedInThePast()
            .setClock(clock)
            .setClockSkew(Duration.ofSeconds(30))
            .build();
  }

  public EndUserPrincipal verify(String compactJwt) throws GeneralSecurityException {
    VerifiedJwt jwt = verify.verifyAndDecode(compactJwt, validator);
    if (!jwt.hasSubject() || jwt.getSubject().isEmpty()) {
      throw new GeneralSecurityException("Token has no subject");
    }
    if (!jwt.hasStringClaim(DemoIdentityProvider.TENANT_CLAIM)) {
      throw new GeneralSecurityException("Token has no tenant");
    }
    Set<String> scopes = new HashSet<>();
    if (jwt.hasStringClaim(DemoIdentityProvider.SCOPE_CLAIM)) {
      scopes.addAll(
          Arrays.asList(jwt.getStringClaim(DemoIdentityProvider.SCOPE_CLAIM).trim().split("\\s+")));
    }
    return new EndUserPrincipal(
        jwt.getIssuer(),
        jwt.getSubject(),
        jwt.getStringClaim(DemoIdentityProvider.TENANT_CLAIM),
        scopes);
  }
}
