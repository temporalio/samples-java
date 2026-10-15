package io.temporal.samples.nexussecurecredentialpropagation.common;

import io.temporal.api.common.v1.Payload;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Hands the digest of an argument list between the payload codec and the credential interceptors
 * through thread locals.
 *
 * <p>The codec knows nothing about credentials. It reports every list it encodes or decodes, and
 * this class decides what to do with the digest:
 *
 * <ul>
 *   <li>Sending side: an interceptor arms a {@link PendingSeal} before calling {@code next}. The
 *       first list encoded while armed (the arguments, since the SDK encodes them before memo or
 *       signal payloads) completes the seal, which writes the credential header into the live
 *       header map that the SDK reads after encoding.
 *   <li>Receiving side: every decoded list publishes its digest, overwriting the previous one. The
 *       receiving interceptor takes it immediately after the SDK decodes the arguments.
 * </ul>
 *
 * <p>This relies on the SDK encoding and decoding arguments synchronously on the interceptor's
 * thread. The sample's tests pin each such ordering.
 */
public final class ArgsDigest {
  private static final ThreadLocal<PendingSeal> PENDING = new ThreadLocal<>();
  private static final ThreadLocal<Received> RECEIVED = new ThreadLocal<>();

  private ArgsDigest() {}

  /**
   * Computes the digest of a list of encoded payloads exactly as transmitted. It covers every
   * ciphertext, its position, and the count.
   */
  public static byte[] compute(List<Payload> encoded) {
    Frame frame = Frame.of("args-digest").add("v1").add(encoded.size());
    for (Payload payload : encoded) {
      frame.add(payload.getData().toByteArray());
    }
    try {
      return MessageDigest.getInstance("SHA-256").digest(frame.toByteArray());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Arms a pending seal on the current thread. */
  public static void arm(PendingSeal seal) {
    if (PENDING.get() != null) {
      throw new IllegalStateException("A credential seal is already pending on this thread");
    }
    PENDING.set(seal);
  }

  /** Removes any pending seal from the current thread. */
  public static void disarm() {
    PENDING.remove();
  }

  /** Called by the codec after encoding a list of payloads. */
  public static void onEncoded(List<Payload> encoded) {
    PendingSeal seal = PENDING.get();
    if (seal == null) {
      return;
    }
    // Only the first list encoded while armed is the argument list.
    PENDING.remove();
    seal.complete(compute(encoded), encoded.size());
  }

  /** Called by the codec after decoding a list of payloads. */
  public static void onDecoded(List<Payload> encoded) {
    RECEIVED.set(new Received(compute(encoded), encoded.size()));
  }

  /** Takes and clears the digest of the most recently decoded list, or null if there is none. */
  public static Received take() {
    Received received = RECEIVED.get();
    RECEIVED.remove();
    return received;
  }

  /** Digest and argument count of a decoded list. */
  public static final class Received {
    private final byte[] digest;
    private final int count;

    Received(byte[] digest, int count) {
      this.digest = digest;
      this.count = count;
    }

    public byte[] getDigest() {
      return digest.clone();
    }

    public int getCount() {
      return count;
    }
  }

  /**
   * A credential header waiting for the digest of the arguments it must be bound to. Subclasses
   * seal the credential and write the header into the map the SDK reads after encoding.
   */
  public abstract static class PendingSeal {
    private boolean completed;

    final void complete(byte[] digest, int count) {
      seal(digest, count);
      completed = true;
    }

    /** Seals the credential for the given argument digest and writes the header. */
    protected abstract void seal(byte[] digest, int count);

    /** Whether the seal was completed, meaning the header was written. */
    public boolean isCompleted() {
      return completed;
    }
  }
}
