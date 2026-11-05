package io.temporal.samples.nexussecurecredentialpropagation.common;

import java.security.GeneralSecurityException;

/**
 * The plaintext sealed into every credential header: the end user's compact JWT.
 *
 * <p>Workflow code treats this as opaque. {@link #toString()} never reveals its contents.
 */
public final class CredentialEnvelope {
  private static final String LABEL = "credential-envelope";
  private static final String VERSION = "v1";
  static final int MAX_JWT_LENGTH = 8 * 1024;

  private final String compactJwt;

  public CredentialEnvelope(String compactJwt) {
    if (compactJwt.isEmpty() || compactJwt.length() > MAX_JWT_LENGTH) {
      throw new IllegalArgumentException("Invalid JWT length");
    }
    this.compactJwt = compactJwt;
  }

  public String getCompactJwt() {
    return compactJwt;
  }

  public byte[] toBytes() {
    return Frame.of(LABEL).add(VERSION).add(compactJwt).toByteArray();
  }

  public static CredentialEnvelope fromBytes(byte[] bytes) throws GeneralSecurityException {
    Frame.Reader reader = new Frame.Reader(bytes);
    reader.expect(LABEL);
    reader.expect(VERSION);
    String jwt = reader.nextString(MAX_JWT_LENGTH);
    if (jwt.isEmpty()) {
      throw new GeneralSecurityException("Empty JWT");
    }
    reader.expectEnd();
    return new CredentialEnvelope(jwt);
  }

  @Override
  public String toString() {
    return "CredentialEnvelope{<redacted>}";
  }
}
