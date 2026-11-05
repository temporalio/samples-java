package io.temporal.samples.nexussecurecredentialpropagation.caller;

import io.temporal.api.common.v1.Payload;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Header map that records whether it was read after the credential was sealed into it.
 *
 * <p>The SDK serializes the header after encoding the arguments. If the map is never read after the
 * seal, the sealed header was not sent, typically because an inner interceptor copied the map
 * earlier.
 */
final class ReadTrackingHeaderMap extends HashMap<String, Payload> {
  private static final long serialVersionUID = 1L;

  private boolean sealed;
  private boolean readAfterSeal;

  ReadTrackingHeaderMap(Map<String, Payload> values) {
    super(values);
  }

  void markSealed() {
    sealed = true;
  }

  boolean wasReadAfterSeal() {
    return readAfterSeal;
  }

  private void recordRead() {
    if (sealed) {
      readAfterSeal = true;
    }
  }

  @Override
  public Set<Map.Entry<String, Payload>> entrySet() {
    recordRead();
    return super.entrySet();
  }

  @Override
  public Set<String> keySet() {
    recordRead();
    return super.keySet();
  }

  @Override
  public Collection<Payload> values() {
    recordRead();
    return super.values();
  }

  @Override
  public void forEach(BiConsumer<? super String, ? super Payload> action) {
    recordRead();
    super.forEach(action);
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
