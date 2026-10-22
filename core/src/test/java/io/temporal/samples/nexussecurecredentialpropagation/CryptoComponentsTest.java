package io.temporal.samples.nexussecurecredentialpropagation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.HybridEncrypt;
import com.google.protobuf.ByteString;
import io.nexusrpc.handler.HandlerException;
import io.temporal.api.common.v1.Payload;
import io.temporal.failure.ApplicationFailure;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.payload.context.NexusSerializationContext;
import io.temporal.payload.context.WorkflowSerializationContext;
import io.temporal.samples.nexussecurecredentialpropagation.common.ArgsDigest;
import io.temporal.samples.nexussecurecredentialpropagation.common.ContextAwareTinkPayloadCodec;
import io.temporal.samples.nexussecurecredentialpropagation.common.CredentialEnvelope;
import io.temporal.samples.nexussecurecredentialpropagation.common.DemoIdentityProvider;
import io.temporal.samples.nexussecurecredentialpropagation.common.EndUserPrincipal;
import io.temporal.samples.nexussecurecredentialpropagation.common.Frame;
import io.temporal.samples.nexussecurecredentialpropagation.common.SampleKeysets;
import io.temporal.samples.nexussecurecredentialpropagation.common.SecureCredentialHeaders;
import io.temporal.samples.nexussecurecredentialpropagation.handler.AuthorizationPolicy;
import io.temporal.samples.nexussecurecredentialpropagation.handler.JwtVerifier;
import io.temporal.samples.nexussecurecredentialpropagation.handler.TokenProbe;
import io.temporal.samples.nexussecurecredentialpropagation.handler.TransferAuthorizationPolicy;
import io.temporal.samples.nexussecurecredentialpropagation.handler.TransferNexusServiceImpl;
import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests of the sample's cryptographic building blocks. */
public class CryptoComponentsTest {
  private static final byte[] DIGEST = new byte[32];

  @TempDir Path keyDir;

  private SampleKeysets keys;
  private CredentialEnvelope envelope;

  @BeforeEach
  public void setUp() throws Exception {
    keys = SampleKeysets.fromDirectory(keyDir);
    keys.generate();
    envelope = new CredentialEnvelope("header.payload.signature");
    ArgsDigest.take();
  }

  // Frame.

  @Test
  public void frameEncodingRespectsFieldBoundaries() {
    assertFalse(
        Arrays.equals(
            Frame.of("x").add("ab").add("c").toByteArray(),
            Frame.of("x").add("a").add("bc").toByteArray()));
  }

  @Test
  public void frameReaderRejectsTruncationAndTrailingBytes() {
    byte[] bytes = Frame.of("x").add("value").toByteArray();
    Frame.Reader truncated = new Frame.Reader(Arrays.copyOf(bytes, bytes.length - 1));
    assertThrows(
        GeneralSecurityException.class,
        () -> {
          truncated.expect("x");
          truncated.nextString(100);
        });
    Frame.Reader trailing = new Frame.Reader(Arrays.copyOf(bytes, bytes.length + 1));
    assertThrows(
        GeneralSecurityException.class,
        () -> {
          trailing.expect("x");
          trailing.nextString(100);
          trailing.expectEnd();
        });
  }

  // Credential headers.

  @Test
  public void credentialHeaderOpensOnlyUnderItsExactContext() throws Exception {
    HybridEncrypt encrypt = keys.nexusCredentialEncrypt();
    HybridDecrypt decrypt = keys.nexusCredentialDecrypt();
    byte[] context = SecureCredentialHeaders.nexusContext("ep", "svc", "op", DIGEST);
    byte[] sealed = SecureCredentialHeaders.seal(encrypt, envelope, context);

    assertEquals(
        envelope.getCompactJwt(),
        SecureCredentialHeaders.open(decrypt, sealed, context).getCompactJwt());

    byte[] otherDigest = DIGEST.clone();
    otherDigest[0] = 1;
    List<byte[]> wrongContexts =
        Arrays.asList(
            SecureCredentialHeaders.nexusContext("other", "svc", "op", DIGEST),
            SecureCredentialHeaders.nexusContext("ep", "other", "op", DIGEST),
            SecureCredentialHeaders.nexusContext("ep", "svc", "other", DIGEST),
            SecureCredentialHeaders.nexusContext("ep", "svc", "op", otherDigest),
            // Same values under another purpose.
            SecureCredentialHeaders.startContext("ep", "svc", "op", DIGEST));
    for (byte[] wrong : wrongContexts) {
      assertThrows(
          GeneralSecurityException.class,
          () -> SecureCredentialHeaders.open(decrypt, sealed, wrong));
    }
  }

  @Test
  public void workflowCredentialKeysCannotOpenNexusHeaders() {
    byte[] context = SecureCredentialHeaders.nexusContext("ep", "svc", "op", DIGEST);
    byte[] sealed = SecureCredentialHeaders.seal(keys.nexusCredentialEncrypt(), envelope, context);
    // The caller Worker holds the Workflow credential private key, not the Nexus one.
    assertThrows(
        GeneralSecurityException.class,
        () -> SecureCredentialHeaders.open(keys.workflowCredentialDecrypt(), sealed, context));
  }

  @Test
  public void continueAsNewHeaderIsBoundToTheRunChain() throws Exception {
    byte[] context = SecureCredentialHeaders.continueAsNewContext("ns", "Type", "wid", "first-run");
    byte[] sealed =
        SecureCredentialHeaders.seal(keys.workflowCredentialEncrypt(), envelope, context);
    SecureCredentialHeaders.open(keys.workflowCredentialDecrypt(), sealed, context);
    assertThrows(
        GeneralSecurityException.class,
        () ->
            SecureCredentialHeaders.open(
                keys.workflowCredentialDecrypt(),
                sealed,
                SecureCredentialHeaders.continueAsNewContext("ns", "Type", "wid", "fresh-run")));
  }

  @Test
  public void nexusHeaderParsingIsStrict() {
    String valid = SecureCredentialHeaders.toNexusHeaderValue(new byte[] {1, 2, 3, 4, 5});
    assertTrue(valid.startsWith("v1."));
    List<String> invalid =
        Arrays.asList(
            valid + "=",
            valid.replace("v1.", "v2."),
            valid + "+",
            "v1.",
            "v1." + repeat('A', 20_000));
    for (String value : invalid) {
      assertThrows(
          GeneralSecurityException.class,
          () -> SecureCredentialHeaders.parseNexusHeaderValue(value),
          value.length() > 40 ? value.substring(0, 40) : value);
    }
  }

  @Test
  public void envelopeRoundTripsAndRejectsTrailingData() throws Exception {
    byte[] bytes = envelope.toBytes();
    CredentialEnvelope decoded = CredentialEnvelope.fromBytes(bytes);
    assertEquals(envelope.getCompactJwt(), decoded.getCompactJwt());
    assertThrows(
        GeneralSecurityException.class,
        () -> CredentialEnvelope.fromBytes(Arrays.copyOf(bytes, bytes.length + 1)));
    assertFalse(envelope.toString().contains(envelope.getCompactJwt()));
  }

  // Args digest.

  @Test
  public void argsDigestCoversEveryCiphertextPositionAndCount() {
    Payload a = payload("a");
    Payload b = payload("b");
    byte[] original = ArgsDigest.compute(Arrays.asList(a, b));
    assertArrayEquals(original, ArgsDigest.compute(Arrays.asList(a, b)));
    assertDifferent(original, ArgsDigest.compute(Arrays.asList(b, a)));
    assertDifferent(original, ArgsDigest.compute(Arrays.asList(a, a)));
    assertDifferent(original, ArgsDigest.compute(Collections.singletonList(a)));
    assertDifferent(original, ArgsDigest.compute(Arrays.asList(a, b, b)));
    assertDifferent(original, ArgsDigest.compute(Arrays.asList(a, payload("c"))));
  }

  @Test
  public void onlyTheFirstListEncodedWhileArmedIsDigested() {
    List<byte[]> sealedDigests = new ArrayList<>();
    ArgsDigest.PendingSeal seal =
        new ArgsDigest.PendingSeal() {
          @Override
          protected void seal(byte[] digest, int count) {
            sealedDigests.add(digest);
          }
        };
    ArgsDigest.arm(seal);
    try {
      ArgsDigest.onEncoded(Collections.singletonList(payload("args")));
      ArgsDigest.onEncoded(Collections.singletonList(payload("memo")));
    } finally {
      ArgsDigest.disarm();
    }
    assertTrue(seal.isCompleted());
    assertEquals(1, sealedDigests.size());
    assertArrayEquals(
        ArgsDigest.compute(Collections.singletonList(payload("args"))), sealedDigests.get(0));
  }

  @Test
  public void receivedDigestIsTakenOnce() {
    ArgsDigest.onDecoded(Collections.singletonList(payload("x")));
    assertEquals(1, ArgsDigest.take().getCount());
    assertNull(ArgsDigest.take());
  }

  // Payload codec.

  @Test
  public void payloadsAreBoundToTheirContext() {
    Aead aead = keys.payloadAead();
    PayloadCodec base = new ContextAwareTinkPayloadCodec(aead);
    PayloadCodec nexus = base.withContext(new NexusSerializationContext("ep", "svc", "op"));
    List<Payload> encoded = nexus.encode(Collections.singletonList(plain("hello")));

    assertEquals("hello", nexus.decode(encoded).get(0).getData().toStringUtf8());
    assertRejected(
        () ->
            base.withContext(new NexusSerializationContext("other", "svc", "op")).decode(encoded));
    assertRejected(
        () -> base.withContext(new WorkflowSerializationContext("ns", "wid")).decode(encoded));

    // Relabelling the stored context fails authentication.
    Payload relabelled =
        encoded.get(0).toBuilder()
            .putMetadata(
                "tink-context",
                ByteString.copyFrom(Frame.of("nexus").add("ep").add("svc").add("x").toByteArray()))
            .build();
    assertRejected(() -> nexus.decode(Collections.singletonList(relabelled)));
  }

  @Test
  public void contextFreePayloadsAreRejectedWhereAContextIsKnown() {
    PayloadCodec base = new ContextAwareTinkPayloadCodec(keys.payloadAead());
    List<Payload> contextFree = base.encode(Collections.singletonList(plain("x")));
    assertEquals("x", base.decode(contextFree).get(0).getData().toStringUtf8());
    assertRejected(
        () ->
            base.withContext(new NexusSerializationContext("ep", "svc", "op")).decode(contextFree));
  }

  @Test
  public void asyncNexusResultsFromTheHandlerWorkflowAreAccepted() {
    PayloadCodec base = new ContextAwareTinkPayloadCodec(keys.payloadAead());
    List<Payload> result =
        base.withContext(new WorkflowSerializationContext("handler-ns", "backing-wid"))
            .encode(Collections.singletonList(plain("result")));
    assertEquals(
        "result",
        base.withContext(new NexusSerializationContext("ep", "svc", "op"))
            .decode(result)
            .get(0)
            .getData()
            .toStringUtf8());
  }

  @Test
  public void tamperedCiphertextIsRejected() {
    PayloadCodec codec =
        new ContextAwareTinkPayloadCodec(keys.payloadAead())
            .withContext(new NexusSerializationContext("ep", "svc", "op"));
    Payload encoded = codec.encode(Collections.singletonList(plain("x"))).get(0);
    byte[] data = encoded.getData().toByteArray();
    data[data.length - 1] ^= 1;
    Payload tampered = encoded.toBuilder().setData(ByteString.copyFrom(data)).build();
    assertRejected(() -> codec.decode(Collections.singletonList(tampered)));
  }

  // JWT validation.

  @Test
  public void jwtValidationRejectsWrongIssuerAudienceAndExpiredTokens() throws Exception {
    DemoIdentityProvider idp = new DemoIdentityProvider(keys.demoIdentityProviderSigner());
    JwtVerifier verifier =
        new JwtVerifier(
            keys.demoIdentityProviderVerifier(),
            DemoIdentityProvider.ISSUER,
            DemoIdentityProvider.HANDLER_AUDIENCE);

    EndUserPrincipal principal =
        verifier.verify(idp.issue("alice", "acme", "payments:transfer", Duration.ofMinutes(5)));
    assertEquals("alice", principal.getSubject());
    assertEquals("acme", principal.getTenant());

    JwtVerifier otherIssuer =
        new JwtVerifier(
            keys.demoIdentityProviderVerifier(),
            "https://other-idp.example.com",
            DemoIdentityProvider.HANDLER_AUDIENCE);
    String token = idp.issue("alice", "acme", "payments:transfer", Duration.ofMinutes(5));
    assertThrows(GeneralSecurityException.class, () -> otherIssuer.verify(token));

    String otherAudience =
        idp.issue(
            "alice", "acme", "payments:transfer", "other", Instant.now(), Duration.ofMinutes(5));
    assertThrows(GeneralSecurityException.class, () -> verifier.verify(otherAudience));

    String expired =
        idp.issue(
            "alice",
            "acme",
            "payments:transfer",
            DemoIdentityProvider.HANDLER_AUDIENCE,
            Instant.now().minus(Duration.ofHours(2)),
            Duration.ofMinutes(5));
    assertThrows(GeneralSecurityException.class, () -> verifier.verify(expired));

    String notYetValid =
        idp.issue(
            "alice",
            "acme",
            "payments:transfer",
            DemoIdentityProvider.HANDLER_AUDIENCE,
            Instant.now().plus(Duration.ofHours(1)),
            Duration.ofMinutes(5));
    assertThrows(GeneralSecurityException.class, () -> verifier.verify(notYetValid));

    int signatureStart = token.lastIndexOf('.') + 1;
    // Change the first signature character to a different one.
    char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
    String forged =
        token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
    assertThrows(GeneralSecurityException.class, () -> verifier.verify(forged));
  }

  // Authorization and idempotency.

  @Test
  public void authorizationPolicyEnforcesPrincipal() throws Exception {
    TransferAuthorizationPolicy policy = new TransferAuthorizationPolicy();
    EndUserPrincipal alice =
        new EndUserPrincipal(
            DemoIdentityProvider.ISSUER,
            "alice",
            "acme",
            Collections.singleton(TransferService.REQUIRED_SCOPE));
    policy.authorize(alice, transfer("acme", "alice", "p-1", "bob", 50_00));

    List<TransferService.TransferInput> denied =
        Arrays.asList(
            transfer("other", "alice", "p-1", "bob", 10_00),
            transfer("acme", "bob", "p-1", "bob", 10_00),
            transfer("acme", "alice", "p-1", "bob", 0));
    for (TransferService.TransferInput input : denied) {
      assertThrows(
          AuthorizationPolicy.AuthorizationDeniedException.class,
          () -> policy.authorize(alice, input));
    }

    EndUserPrincipal noScope =
        new EndUserPrincipal(DemoIdentityProvider.ISSUER, "alice", "acme", Collections.emptySet());
    assertThrows(
        AuthorizationPolicy.AuthorizationDeniedException.class,
        () -> policy.authorize(noScope, transfer("acme", "alice", "p-1", "bob", 10_00)));
  }

  @Test
  public void resourceIdDependsOnPrincipalAndBusinessIdentityOnly() {
    EndUserPrincipal alice =
        new EndUserPrincipal(DemoIdentityProvider.ISSUER, "alice", "acme", Collections.emptySet());
    EndUserPrincipal bob =
        new EndUserPrincipal(DemoIdentityProvider.ISSUER, "bob", "acme", Collections.emptySet());
    TransferService.TransferInput input = transfer("acme", "alice", "p-1", "bob", 10_00);

    assertEquals(
        TransferNexusServiceImpl.resourceId(alice, input),
        TransferNexusServiceImpl.resourceId(alice, transfer("acme", "alice", "p-1", "bob", 20_00)));
    assertNotEquals(
        TransferNexusServiceImpl.resourceId(alice, input),
        TransferNexusServiceImpl.resourceId(bob, input));
    assertFalse(TransferNexusServiceImpl.resourceId(alice, input).contains("alice"));
  }

  @Test
  public void operationTokensCannotBeForged() throws Exception {
    SampleKeysets otherKeys = SampleKeysets.fromDirectory(keyDir.resolve("other"));
    otherKeys.generate();
    TokenProbe ours = new TokenProbe(keys);
    TokenProbe theirs = new TokenProbe(otherKeys);

    String token = ours.sign("transfer-123");
    assertEquals("transfer-123", ours.verify(token));
    assertThrows(HandlerException.class, () -> ours.verify(theirs.sign("transfer-123")));
    assertThrows(HandlerException.class, () -> ours.verify(token.substring(0, token.length() - 2)));
    assertThrows(HandlerException.class, () -> ours.verify("not a token"));
  }

  // Helpers.

  private static TransferService.TransferInput transfer(
      String tenant, String user, String paymentId, String beneficiary, long amountCents) {
    return new TransferService.TransferInput(tenant, user, paymentId, beneficiary, amountCents);
  }

  private static Payload payload(String data) {
    return Payload.newBuilder().setData(ByteString.copyFromUtf8(data)).build();
  }

  private static Payload plain(String data) {
    return Payload.newBuilder()
        .putMetadata("encoding", ByteString.copyFromUtf8("binary/plain"))
        .setData(ByteString.copyFromUtf8(data))
        .build();
  }

  private static void assertDifferent(byte[] expected, byte[] actual) {
    assertFalse(Arrays.equals(expected, actual));
  }

  private static void assertRejected(Runnable decode) {
    ApplicationFailure failure = assertThrows(ApplicationFailure.class, decode::run);
    assertTrue(failure.isNonRetryable());
    assertEquals("PayloadValidationError", failure.getType());
  }

  private static String repeat(char c, int count) {
    char[] chars = new char[count];
    Arrays.fill(chars, c);
    return new String(chars);
  }
}
