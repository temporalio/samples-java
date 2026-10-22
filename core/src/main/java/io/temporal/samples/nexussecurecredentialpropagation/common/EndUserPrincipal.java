package io.temporal.samples.nexussecurecredentialpropagation.common;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/** The verified end user on whose behalf a Nexus Operation runs. */
public final class EndUserPrincipal {
  private final String issuer;
  private final String subject;
  private final String tenant;
  private final Set<String> scopes;

  public EndUserPrincipal(String issuer, String subject, String tenant, Set<String> scopes) {
    this.issuer = issuer;
    this.subject = subject;
    this.tenant = tenant;
    this.scopes = Collections.unmodifiableSet(new TreeSet<>(scopes));
  }

  public String getIssuer() {
    return issuer;
  }

  public String getSubject() {
    return subject;
  }

  public String getTenant() {
    return tenant;
  }

  public Set<String> getScopes() {
    return scopes;
  }

  @Override
  public String toString() {
    // Identifiers only; never the token or other claims.
    return "EndUserPrincipal{issuer='"
        + issuer
        + "', subject='"
        + subject
        + "', tenant='"
        + tenant
        + "'}";
  }
}
