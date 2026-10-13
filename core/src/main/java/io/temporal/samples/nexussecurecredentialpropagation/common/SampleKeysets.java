package io.temporal.samples.nexussecurecredentialpropagation.common;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.HybridEncrypt;
import com.google.crypto.tink.InsecureSecretKeyAccess;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.Mac;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.TinkJsonProtoKeysetFormat;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.PredefinedAeadParameters;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.crypto.tink.jwt.JwtPublicKeySign;
import com.google.crypto.tink.jwt.JwtPublicKeyVerify;
import com.google.crypto.tink.jwt.JwtSignatureConfig;
import com.google.crypto.tink.mac.MacConfig;
import com.google.crypto.tink.mac.PredefinedMacParameters;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

/**
 * Demo keysets for the sample.
 *
 * <p>Keysets are stored as cleartext Tink JSON in a local directory so the three demo processes can
 * share them. This is for demonstration only. In production, keysets are encrypted with a KMS key
 * ({@code TinkJsonProtoKeysetFormat.parseEncryptedKeyset}) and each process is given only the
 * keysets its role requires:
 *
 * <table>
 *   <caption>Keysets by role</caption>
 *   <tr><th>Keyset</th><th>Starter</th><th>Caller Worker</th><th>Handler Worker</th></tr>
 *   <tr><td>payload (AEAD)</td><td>yes</td><td>yes</td><td>yes</td></tr>
 *   <tr><td>workflow credential (HPKE)</td><td>public</td><td>private + public</td><td>-</td></tr>
 *   <tr><td>nexus credential (HPKE)</td><td>-</td><td>public</td><td>private</td></tr>
 *   <tr><td>operation token (MAC)</td><td>-</td><td>-</td><td>yes</td></tr>
 *   <tr><td>demo identity provider (JWT ES256)</td><td>private</td><td>-</td><td>public</td></tr>
 * </table>
 */
public final class SampleKeysets {
  public static final String KEY_DIR_PROPERTY = "nexus.secure.credential.keyDir";
  private static final String DEFAULT_KEY_DIR = "build/nexus-secure-credential-keys";

  static final String PAYLOAD = "payload.json";
  static final String WORKFLOW_CREDENTIAL_PRIVATE = "workflow-credential.private.json";
  static final String WORKFLOW_CREDENTIAL_PUBLIC = "workflow-credential.public.json";
  static final String NEXUS_CREDENTIAL_PRIVATE = "nexus-credential.private.json";
  static final String NEXUS_CREDENTIAL_PUBLIC = "nexus-credential.public.json";
  static final String OPERATION_TOKEN = "operation-token.json";
  static final String JWT_SIGNING_PRIVATE = "jwt-signing.private.json";
  static final String JWT_SIGNING_PUBLIC = "jwt-signing.public.json";

  private static final String HPKE_TEMPLATE = "DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM";
  private static final String JWT_TEMPLATE = "JWT_ES256";

  private static volatile boolean registered;

  private final Path dir;

  private SampleKeysets(Path dir) {
    this.dir = dir;
  }

  /** Registers the Tink primitives used by the sample. Safe to call repeatedly. */
  public static synchronized void register() {
    if (registered) {
      return;
    }
    try {
      AeadConfig.register();
      HybridConfig.register();
      MacConfig.register();
      JwtSignatureConfig.register();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
    registered = true;
  }

  /** Opens the key directory configured by the {@value #KEY_DIR_PROPERTY} system property. */
  public static SampleKeysets fromDefaultDirectory() {
    return fromDirectory(Paths.get(System.getProperty(KEY_DIR_PROPERTY, DEFAULT_KEY_DIR)));
  }

  public static SampleKeysets fromDirectory(Path dir) {
    register();
    return new SampleKeysets(dir);
  }

  /** Generates every keyset into the directory, replacing any existing files. */
  public void generate() throws IOException, GeneralSecurityException {
    Files.createDirectories(dir);
    writeSecret(PAYLOAD, KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM));
    writePair(
        WORKFLOW_CREDENTIAL_PRIVATE,
        WORKFLOW_CREDENTIAL_PUBLIC,
        KeysetHandle.generateNew(KeyTemplates.get(HPKE_TEMPLATE)));
    writePair(
        NEXUS_CREDENTIAL_PRIVATE,
        NEXUS_CREDENTIAL_PUBLIC,
        KeysetHandle.generateNew(KeyTemplates.get(HPKE_TEMPLATE)));
    writeSecret(
        OPERATION_TOKEN, KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG));
    writePair(
        JWT_SIGNING_PRIVATE,
        JWT_SIGNING_PUBLIC,
        KeysetHandle.generateNew(KeyTemplates.get(JWT_TEMPLATE)));
  }

  public Aead payloadAead() {
    return primitive(readSecret(PAYLOAD), Aead.class);
  }

  public HybridEncrypt workflowCredentialEncrypt() {
    return primitive(readPublic(WORKFLOW_CREDENTIAL_PUBLIC), HybridEncrypt.class);
  }

  public HybridDecrypt workflowCredentialDecrypt() {
    return primitive(readSecret(WORKFLOW_CREDENTIAL_PRIVATE), HybridDecrypt.class);
  }

  public HybridEncrypt nexusCredentialEncrypt() {
    return primitive(readPublic(NEXUS_CREDENTIAL_PUBLIC), HybridEncrypt.class);
  }

  public HybridDecrypt nexusCredentialDecrypt() {
    return primitive(readSecret(NEXUS_CREDENTIAL_PRIVATE), HybridDecrypt.class);
  }

  public Mac operationTokenMac() {
    return primitive(readSecret(OPERATION_TOKEN), Mac.class);
  }

  public JwtPublicKeySign demoIdentityProviderSigner() {
    return primitive(readSecret(JWT_SIGNING_PRIVATE), JwtPublicKeySign.class);
  }

  public JwtPublicKeyVerify demoIdentityProviderVerifier() {
    return primitive(readPublic(JWT_SIGNING_PUBLIC), JwtPublicKeyVerify.class);
  }

  private void writeSecret(String name, KeysetHandle handle)
      throws IOException, GeneralSecurityException {
    write(name, TinkJsonProtoKeysetFormat.serializeKeyset(handle, InsecureSecretKeyAccess.get()));
  }

  private void writePair(String privateName, String publicName, KeysetHandle handle)
      throws IOException, GeneralSecurityException {
    writeSecret(privateName, handle);
    write(
        publicName,
        TinkJsonProtoKeysetFormat.serializeKeysetWithoutSecret(handle.getPublicKeysetHandle()));
  }

  private void write(String name, String json) throws IOException {
    Files.write(dir.resolve(name), json.getBytes(StandardCharsets.UTF_8));
  }

  private KeysetHandle readSecret(String name) {
    try {
      return TinkJsonProtoKeysetFormat.parseKeyset(read(name), InsecureSecretKeyAccess.get());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Invalid keyset " + name, e);
    }
  }

  private KeysetHandle readPublic(String name) {
    try {
      return TinkJsonProtoKeysetFormat.parseKeysetWithoutSecret(read(name));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Invalid keyset " + name, e);
    }
  }

  private String read(String name) {
    Path path = dir.resolve(name);
    try {
      return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Missing keyset " + path + ". Run GenerateDemoKeys first.", e);
    }
  }

  private static <P> P primitive(KeysetHandle handle, Class<P> type) {
    try {
      return handle.getPrimitive(RegistryConfiguration.get(), type);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
