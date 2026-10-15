package io.temporal.samples.nexussecurecredentialpropagation.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.crypto.tink.HybridDecrypt;
import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Payload;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.api.history.v1.NexusOperationScheduledEventAttributes;
import io.temporal.api.history.v1.WorkflowExecutionStartedEventAttributes;
import io.temporal.client.WorkflowClient;
import io.temporal.samples.nexus.options.ClientOptions;
import io.temporal.samples.nexussecurecredentialpropagation.common.ArgsDigest;
import io.temporal.samples.nexussecurecredentialpropagation.common.CredentialEnvelope;
import io.temporal.samples.nexussecurecredentialpropagation.common.SampleKeysets;
import io.temporal.samples.nexussecurecredentialpropagation.common.SecureCredentialHeaders;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Checks, against a real Workflow history, that every credential header is bound to its context.
 *
 * <p>For each {@code WorkflowExecutionStarted} and {@code NexusOperationScheduled} event that
 * carries a credential header, it rebuilds the context the receiver would use (recomputing the args
 * digest from the event's input ciphertexts, or using {@code firstExecutionRunId} for
 * continue-as-new headers), opens the header with it, and confirms that the same context with one
 * bit changed is rejected. It also confirms the digest never appears in the header bytes: the
 * digest is authenticated associated data, not a transmitted field.
 *
 * <p>Opening a header requires the receivers' private keys, so this is an operator debugging tool.
 * It prints only the subject and issuer of each credential, never the token.
 */
public final class VerifyCredentialBinding {
  public static final String WORKFLOW_ID_PROPERTY = "nexus.secure.credential.workflowId";
  private static final String DEFAULT_WORKFLOW_ID = "transfer-payment-1001";
  private static final ObjectMapper JSON = new ObjectMapper();

  private final HybridDecrypt workflowCredentialDecrypt;
  private final HybridDecrypt nexusCredentialDecrypt;

  public VerifyCredentialBinding(
      HybridDecrypt workflowCredentialDecrypt, HybridDecrypt nexusCredentialDecrypt) {
    this.workflowCredentialDecrypt = workflowCredentialDecrypt;
    this.nexusCredentialDecrypt = nexusCredentialDecrypt;
  }

  /** Outcome of verifying one credential header. */
  public static final class Result {
    private final long eventId;
    private final String eventType;
    private final List<String> details;
    private final boolean opensWithContext;
    private final boolean rejectsTamperedContext;
    private final boolean bindingValueHidden;

    Result(
        long eventId,
        String eventType,
        List<String> details,
        boolean opensWithContext,
        boolean rejectsTamperedContext,
        boolean bindingValueHidden) {
      this.eventId = eventId;
      this.eventType = eventType;
      this.details = details;
      this.opensWithContext = opensWithContext;
      this.rejectsTamperedContext = rejectsTamperedContext;
      this.bindingValueHidden = bindingValueHidden;
    }

    public long getEventId() {
      return eventId;
    }

    public String getEventType() {
      return eventType;
    }

    public boolean passed() {
      return opensWithContext && rejectsTamperedContext && bindingValueHidden;
    }

    String describe() {
      StringBuilder out = new StringBuilder();
      out.append("=== Event ").append(eventId).append(' ').append(eventType).append('\n');
      for (String detail : details) {
        out.append("  ").append(detail).append('\n');
      }
      out.append("  binding value absent from header bytes: ")
          .append(bindingValueHidden)
          .append('\n');
      out.append("  opens with recomputed context:          ")
          .append(opensWithContext)
          .append('\n');
      out.append("  rejects context with one bit changed:   ")
          .append(rejectsTamperedContext)
          .append('\n');
      out.append("  ").append(passed() ? "PASS" : "FAIL").append('\n');
      return out.toString();
    }
  }

  /**
   * Verifies every credential header in one run's history.
   *
   * @param namespace namespace the history belongs to
   * @param workflowId Workflow ID of the run
   */
  public List<Result> verify(String namespace, String workflowId, List<HistoryEvent> events) {
    List<Result> results = new ArrayList<>();
    for (HistoryEvent event : events) {
      if (event.hasWorkflowExecutionStartedEventAttributes()) {
        WorkflowExecutionStartedEventAttributes started =
            event.getWorkflowExecutionStartedEventAttributes();
        Payload header =
            started.getHeader().getFieldsMap().get(SecureCredentialHeaders.WORKFLOW_HEADER);
        if (header != null) {
          results.add(verifyStart(event.getEventId(), namespace, workflowId, started, header));
        }
      }
      if (event.hasNexusOperationScheduledEventAttributes()) {
        NexusOperationScheduledEventAttributes scheduled =
            event.getNexusOperationScheduledEventAttributes();
        String header = scheduled.getNexusHeaderMap().get(SecureCredentialHeaders.NEXUS_HEADER);
        if (header != null) {
          results.add(verifyNexus(event.getEventId(), scheduled, header));
        }
      }
    }
    return results;
  }

  private Result verifyStart(
      long eventId,
      String namespace,
      String workflowId,
      WorkflowExecutionStartedEventAttributes started,
      Payload header) {
    List<String> details = new ArrayList<>();
    String type = started.getWorkflowType().getName();
    details.add("header end-user-credential = " + abbreviate(header.getData().toStringUtf8()));
    SecureCredentialHeaders.WorkflowHeader parsed;
    try {
      parsed = SecureCredentialHeaders.parseWorkflowHeader(header);
    } catch (GeneralSecurityException e) {
      details.add("unparseable header: " + e.getMessage());
      return new Result(eventId, "WorkflowExecutionStarted", details, false, false, false);
    }
    byte[] ciphertext = parsed.getCiphertext();
    byte[] binding;
    byte[] context;
    byte[] tampered;
    if (parsed.getKind() == SecureCredentialHeaders.WorkflowHeaderKind.START) {
      List<Payload> input = started.getInput().getPayloadsList();
      describeInput(input, details);
      binding = ArgsDigest.compute(input);
      details.add("args digest recomputed from input = " + hex(binding));
      context = SecureCredentialHeaders.startContext(namespace, type, workflowId, binding);
      tampered = SecureCredentialHeaders.startContext(namespace, type, workflowId, flip(binding));
    } else {
      String firstRunId = started.getFirstExecutionRunId();
      binding = firstRunId.getBytes(StandardCharsets.UTF_8);
      details.add("continue-as-new header bound to firstExecutionRunId = " + firstRunId);
      context =
          SecureCredentialHeaders.continueAsNewContext(namespace, type, workflowId, firstRunId);
      tampered =
          SecureCredentialHeaders.continueAsNewContext(
              namespace, type, workflowId, new String(flip(binding), StandardCharsets.UTF_8));
    }
    return finish(
        eventId,
        "WorkflowExecutionStarted",
        details,
        workflowCredentialDecrypt,
        ciphertext,
        context,
        tampered,
        binding);
  }

  private Result verifyNexus(
      long eventId, NexusOperationScheduledEventAttributes scheduled, String header) {
    List<String> details = new ArrayList<>();
    details.add(
        "route = "
            + scheduled.getEndpoint()
            + " / "
            + scheduled.getService()
            + " / "
            + scheduled.getOperation());
    details.add("header " + SecureCredentialHeaders.NEXUS_HEADER + " = " + abbreviate(header));
    byte[] ciphertext;
    try {
      ciphertext = SecureCredentialHeaders.parseNexusHeaderValue(header);
    } catch (GeneralSecurityException e) {
      details.add("unparseable header: " + e.getMessage());
      return new Result(eventId, "NexusOperationScheduled", details, false, false, false);
    }
    List<Payload> input = Collections.singletonList(scheduled.getInput());
    describeInput(input, details);
    byte[] digest = ArgsDigest.compute(input);
    details.add("args digest recomputed from input = " + hex(digest));
    return finish(
        eventId,
        "NexusOperationScheduled",
        details,
        nexusCredentialDecrypt,
        ciphertext,
        SecureCredentialHeaders.nexusContext(
            scheduled.getEndpoint(), scheduled.getService(), scheduled.getOperation(), digest),
        SecureCredentialHeaders.nexusContext(
            scheduled.getEndpoint(),
            scheduled.getService(),
            scheduled.getOperation(),
            flip(digest)),
        digest);
  }

  private static Result finish(
      long eventId,
      String eventType,
      List<String> details,
      HybridDecrypt decrypt,
      byte[] ciphertext,
      byte[] context,
      byte[] tamperedContext,
      byte[] binding) {
    boolean opens;
    try {
      CredentialEnvelope envelope = SecureCredentialHeaders.open(decrypt, ciphertext, context);
      opens = true;
      details.add("credential: " + summarize(envelope.getCompactJwt()));
    } catch (GeneralSecurityException e) {
      opens = false;
    }
    boolean rejectsTampered;
    try {
      SecureCredentialHeaders.open(decrypt, ciphertext, tamperedContext);
      rejectsTampered = false;
    } catch (GeneralSecurityException e) {
      rejectsTampered = true;
    }
    return new Result(
        eventId, eventType, details, opens, rejectsTampered, !contains(ciphertext, binding));
  }

  private static void describeInput(List<Payload> input, List<String> details) {
    for (int i = 0; i < input.size(); i++) {
      Payload payload = input.get(i);
      details.add(
          "input["
              + i
              + "] encoding="
              + payload.getMetadataOrDefault("encoding", ByteString.EMPTY).toStringUtf8()
              + ", ciphertext "
              + payload.getData().size()
              + " bytes");
    }
  }

  /** Subject and issuer only; the token itself is never printed. */
  private static String summarize(String compactJwt) {
    try {
      JsonNode claims =
          JSON.readTree(
              Base64.getUrlDecoder()
                  .decode(
                      compactJwt.substring(
                          compactJwt.indexOf('.') + 1, compactJwt.lastIndexOf('.'))));
      return "sub=" + claims.path("sub").asText() + ", iss=" + claims.path("iss").asText();
    } catch (Exception e) {
      return "<unparseable token>";
    }
  }

  private static byte[] flip(byte[] value) {
    byte[] copy = value.clone();
    copy[0] ^= 1;
    return copy;
  }

  private static boolean contains(byte[] haystack, byte[] needle) {
    outer:
    for (int i = 0; i + needle.length <= haystack.length; i++) {
      for (int j = 0; j < needle.length; j++) {
        if (haystack[i + j] != needle[j]) {
          continue outer;
        }
      }
      return true;
    }
    return false;
  }

  private static String abbreviate(String value) {
    return value.length() <= 60
        ? value
        : value.substring(0, 48) + "...(" + value.length() + " chars)";
  }

  private static String hex(byte[] bytes) {
    StringBuilder out = new StringBuilder();
    for (byte b : bytes) {
      out.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
    }
    return out.toString();
  }

  public static void main(String[] args) {
    SampleKeysets keys = SampleKeysets.fromDefaultDirectory();
    WorkflowClient client = ClientOptions.getWorkflowClient(args);
    String namespace = client.getOptions().getNamespace();
    String workflowId = System.getProperty(WORKFLOW_ID_PROPERTY, DEFAULT_WORKFLOW_ID);

    List<Result> results =
        new VerifyCredentialBinding(keys.workflowCredentialDecrypt(), keys.nexusCredentialDecrypt())
            .verify(namespace, workflowId, client.fetchHistory(workflowId).getEvents());
    boolean allPassed = !results.isEmpty();
    for (Result result : results) {
      System.out.print(result.describe());
      allPassed &= result.passed();
    }
    if (results.isEmpty()) {
      System.out.println("No credential headers found in " + workflowId);
    }
    System.out.println(allPassed ? "All credential headers are bound." : "Verification FAILED.");
    System.exit(allPassed ? 0 : 1);
  }
}
