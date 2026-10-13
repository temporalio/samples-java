package io.temporal.samples.nexussecurecredentialpropagation.common;

import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.HybridEncrypt;
import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.EncodingKeys;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * Wire formats and associated data for the credential headers.
 *
 * <p>Every header carries an HPKE ciphertext of a {@link CredentialEnvelope} encrypted to the
 * receiving hop. The HPKE context info binds it to the hop's context and, where available, to the
 * digest of the exact argument ciphertexts it accompanied. Holding a public key or the payload key
 * is not enough to decrypt a header or to pair it with other arguments.
 */
public final class SecureCredentialHeaders {
  /** Temporal header carrying the Workflow credential on start paths and continue-as-new. */
  public static final String WORKFLOW_HEADER = "end-user-credential";

  /** Nexus header carrying the credential to the Nexus handler. */
  public static final String NEXUS_HEADER = "x-temporal-end-user-credential";

  static final String START_PREFIX = "v1.start.";
  static final String CAN_PREFIX = "v1.can.";
  static final String NEXUS_PREFIX = "v1.";

  static final int MAX_HEADER_LENGTH = 16 * 1024;
  private static final String VERSION = "v1";

  private SecureCredentialHeaders() {}

  /** Kinds of Workflow credential header. */
  public enum WorkflowHeaderKind {
    START,
    CONTINUE_AS_NEW
  }

  /** A parsed Workflow credential header. */
  public static final class WorkflowHeader {
    private final WorkflowHeaderKind kind;
    private final byte[] ciphertext;

    WorkflowHeader(WorkflowHeaderKind kind, byte[] ciphertext) {
      this.kind = kind;
      this.ciphertext = ciphertext;
    }

    public WorkflowHeaderKind getKind() {
      return kind;
    }

    public byte[] getCiphertext() {
      return ciphertext.clone();
    }
  }

  public static byte[] startContext(
      String namespace, String workflowType, String workflowId, byte[] argsDigest) {
    return Frame.of("workflow-credential-start")
        .add(VERSION)
        .add(namespace)
        .add(workflowType)
        .add(workflowId)
        .add(argsDigest)
        .toByteArray();
  }

  public static byte[] continueAsNewContext(
      String namespace, String workflowType, String workflowId, String firstExecutionRunId) {
    return Frame.of("workflow-credential-can")
        .add(VERSION)
        .add(namespace)
        .add(workflowType)
        .add(workflowId)
        .add(firstExecutionRunId)
        .toByteArray();
  }

  public static byte[] nexusContext(
      String endpoint, String service, String operation, byte[] argsDigest) {
    return Frame.of("nexus-credential")
        .add(VERSION)
        .add(endpoint)
        .add(service)
        .add(operation)
        .add(argsDigest)
        .toByteArray();
  }

  public static byte[] seal(HybridEncrypt encrypt, CredentialEnvelope envelope, byte[] context) {
    try {
      return encrypt.encrypt(envelope.toBytes(), context);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Failed to seal credential", e);
    }
  }

  public static CredentialEnvelope open(HybridDecrypt decrypt, byte[] ciphertext, byte[] context)
      throws GeneralSecurityException {
    return CredentialEnvelope.fromBytes(decrypt.decrypt(ciphertext, context));
  }

  public static Payload toWorkflowHeaderPayload(WorkflowHeaderKind kind, byte[] ciphertext) {
    String prefix = kind == WorkflowHeaderKind.START ? START_PREFIX : CAN_PREFIX;
    return Payload.newBuilder()
        .putMetadata(EncodingKeys.METADATA_ENCODING_KEY, ByteString.copyFromUtf8("binary/plain"))
        .setData(ByteString.copyFromUtf8(prefix + encode(ciphertext)))
        .build();
  }

  public static WorkflowHeader parseWorkflowHeader(Payload payload)
      throws GeneralSecurityException {
    if (payload.getData().size() > MAX_HEADER_LENGTH) {
      throw new GeneralSecurityException("Credential header too large");
    }
    String value = payload.getData().toString(StandardCharsets.UTF_8);
    if (value.startsWith(START_PREFIX)) {
      return new WorkflowHeader(
          WorkflowHeaderKind.START, decode(value.substring(START_PREFIX.length())));
    }
    if (value.startsWith(CAN_PREFIX)) {
      return new WorkflowHeader(
          WorkflowHeaderKind.CONTINUE_AS_NEW, decode(value.substring(CAN_PREFIX.length())));
    }
    throw new GeneralSecurityException("Unknown credential header version");
  }

  public static String toNexusHeaderValue(byte[] ciphertext) {
    return NEXUS_PREFIX + encode(ciphertext);
  }

  public static byte[] parseNexusHeaderValue(String value) throws GeneralSecurityException {
    if (value.length() > MAX_HEADER_LENGTH) {
      throw new GeneralSecurityException("Credential header too large");
    }
    if (!value.startsWith(NEXUS_PREFIX)) {
      throw new GeneralSecurityException("Unknown credential header version");
    }
    return decode(value.substring(NEXUS_PREFIX.length()));
  }

  static String encode(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** Strict unpadded Base64URL decoding. */
  static byte[] decode(String value) throws GeneralSecurityException {
    if (value.isEmpty()) {
      throw new GeneralSecurityException("Empty credential header");
    }
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      boolean valid =
          (c >= 'A' && c <= 'Z')
              || (c >= 'a' && c <= 'z')
              || (c >= '0' && c <= '9')
              || c == '-'
              || c == '_';
      if (!valid) {
        throw new GeneralSecurityException("Invalid Base64URL in credential header");
      }
    }
    try {
      byte[] decoded = Base64.getUrlDecoder().decode(value);
      // Reject non-canonical encodings, whose unused trailing bits are not zero.
      if (!encode(decoded).equals(value)) {
        throw new GeneralSecurityException("Non-canonical Base64URL in credential header");
      }
      return decoded;
    } catch (IllegalArgumentException e) {
      throw new GeneralSecurityException("Invalid Base64URL in credential header", e);
    }
  }
}
