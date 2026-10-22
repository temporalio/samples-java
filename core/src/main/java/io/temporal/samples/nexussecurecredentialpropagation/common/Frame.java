package io.temporal.samples.nexussecurecredentialpropagation.common;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * Unambiguous length-prefixed encoding used for associated data, HPKE context info, digests, and
 * envelopes.
 *
 * <p>Every field is written as a 4-byte big-endian length followed by its bytes, so no two
 * different field sequences produce the same encoding.
 */
public final class Frame {
  private final ByteArrayOutputStream out = new ByteArrayOutputStream();

  private Frame() {}

  /** Starts a frame whose first field is the given purpose label. */
  public static Frame of(String label) {
    return new Frame().add(label);
  }

  public Frame add(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Frame fields must not be null");
    }
    return add(value.getBytes(StandardCharsets.UTF_8));
  }

  public Frame add(byte[] value) {
    if (value == null) {
      throw new IllegalArgumentException("Frame fields must not be null");
    }
    out.write(ByteBuffer.allocate(4).putInt(value.length).array(), 0, 4);
    out.write(value, 0, value.length);
    return this;
  }

  public Frame add(long value) {
    return add(ByteBuffer.allocate(8).putLong(value).array());
  }

  public byte[] toByteArray() {
    return out.toByteArray();
  }

  /** Strict reader for frames produced by {@link Frame}. */
  public static final class Reader {
    private final ByteBuffer buffer;

    public Reader(byte[] bytes) {
      this.buffer = ByteBuffer.wrap(bytes);
    }

    public byte[] next(int maxLength) throws GeneralSecurityException {
      if (buffer.remaining() < 4) {
        throw new GeneralSecurityException("Truncated frame");
      }
      int length = buffer.getInt();
      if (length < 0 || length > maxLength || length > buffer.remaining()) {
        throw new GeneralSecurityException("Invalid frame field length");
      }
      byte[] value = new byte[length];
      buffer.get(value);
      return value;
    }

    public String nextString(int maxLength) throws GeneralSecurityException {
      return new String(next(maxLength), StandardCharsets.UTF_8);
    }

    public long nextLong() throws GeneralSecurityException {
      byte[] value = next(8);
      if (value.length != 8) {
        throw new GeneralSecurityException("Invalid frame integer");
      }
      return ByteBuffer.wrap(value).getLong();
    }

    public void expect(String label) throws GeneralSecurityException {
      if (!label.equals(nextString(label.length()))) {
        throw new GeneralSecurityException("Unexpected frame label");
      }
    }

    public boolean hasRemaining() {
      return buffer.hasRemaining();
    }

    public void expectEnd() throws GeneralSecurityException {
      if (buffer.hasRemaining()) {
        throw new GeneralSecurityException("Trailing bytes in frame");
      }
    }
  }
}
