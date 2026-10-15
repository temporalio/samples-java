package io.temporal.samples.nexussecurecredentialpropagation.common;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * What the end user authorized the Workflow to do, sealed by the client alongside the credential.
 *
 * <p>The caller Workflow carries the intent unchanged and never parses it. The handler requires the
 * route it serves to appear in the intent and the typed operation input to satisfy that route's
 * constraints. This bounds what signals, updates, or activity results can steer the Workflow into
 * sending.
 */
public final class CredentialIntent {
  private static final int MAX_GRANTS = 16;
  private static final int MAX_CONSTRAINTS = 32;
  private static final int MAX_FIELD_LENGTH = 1024;

  private final List<RouteGrant> grants;

  public CredentialIntent(List<RouteGrant> grants) {
    if (grants.isEmpty() || grants.size() > MAX_GRANTS) {
      throw new IllegalArgumentException("An intent must contain between 1 and 16 grants");
    }
    this.grants = Collections.unmodifiableList(new ArrayList<>(grants));
  }

  public List<RouteGrant> getGrants() {
    return grants;
  }

  /** Returns the grant for the given route, if the intent contains one. */
  public Optional<RouteGrant> grantFor(String endpoint, String service, String operation) {
    for (RouteGrant grant : grants) {
      if (grant.endpoint.equals(endpoint)
          && grant.service.equals(service)
          && grant.operation.equals(operation)) {
        return Optional.of(grant);
      }
    }
    return Optional.empty();
  }

  void writeTo(Frame frame) {
    frame.add(grants.size());
    for (RouteGrant grant : grants) {
      frame.add(grant.endpoint).add(grant.service).add(grant.operation);
      frame.add(grant.constraints.size());
      for (Map.Entry<String, String> constraint : grant.constraints.entrySet()) {
        frame.add(constraint.getKey()).add(constraint.getValue());
      }
    }
  }

  static CredentialIntent readFrom(Frame.Reader reader) throws GeneralSecurityException {
    int grantCount = readCount(reader, MAX_GRANTS);
    if (grantCount == 0) {
      throw new GeneralSecurityException("Empty intent");
    }
    List<RouteGrant> grants = new ArrayList<>(grantCount);
    for (int i = 0; i < grantCount; i++) {
      String endpoint = reader.nextString(MAX_FIELD_LENGTH);
      String service = reader.nextString(MAX_FIELD_LENGTH);
      String operation = reader.nextString(MAX_FIELD_LENGTH);
      int constraintCount = readCount(reader, MAX_CONSTRAINTS);
      Map<String, String> constraints = new TreeMap<>();
      for (int j = 0; j < constraintCount; j++) {
        String key = reader.nextString(MAX_FIELD_LENGTH);
        String value = reader.nextString(MAX_FIELD_LENGTH);
        if (constraints.put(key, value) != null) {
          throw new GeneralSecurityException("Duplicate intent constraint");
        }
      }
      grants.add(new RouteGrant(endpoint, service, operation, constraints));
    }
    return new CredentialIntent(grants);
  }

  private static int readCount(Frame.Reader reader, int max) throws GeneralSecurityException {
    long count = reader.nextLong();
    if (count < 0 || count > max) {
      throw new GeneralSecurityException("Count out of range");
    }
    return (int) count;
  }

  /** Permission to call one protected Nexus route, with constraints on its input. */
  public static final class RouteGrant {
    private final String endpoint;
    private final String service;
    private final String operation;
    private final Map<String, String> constraints;

    public RouteGrant(
        String endpoint, String service, String operation, Map<String, String> constraints) {
      this.endpoint = Objects.requireNonNull(endpoint);
      this.service = Objects.requireNonNull(service);
      this.operation = Objects.requireNonNull(operation);
      if (constraints.size() > MAX_CONSTRAINTS) {
        throw new IllegalArgumentException("Too many intent constraints");
      }
      // Sorted so the encoding is independent of the caller's map ordering.
      this.constraints = Collections.unmodifiableMap(new TreeMap<>(constraints));
    }

    public String getEndpoint() {
      return endpoint;
    }

    public String getService() {
      return service;
    }

    public String getOperation() {
      return operation;
    }

    public Map<String, String> getConstraints() {
      return constraints;
    }
  }
}
