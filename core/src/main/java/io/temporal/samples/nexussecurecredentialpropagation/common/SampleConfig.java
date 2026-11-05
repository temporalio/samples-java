package io.temporal.samples.nexussecurecredentialpropagation.common;

import com.google.crypto.tink.Aead;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.samples.nexussecurecredentialpropagation.service.TransferService;
import java.util.Collections;
import java.util.Set;

/** Names and wiring shared by the sample's processes. */
public final class SampleConfig {
  public static final String ENDPOINT = "nexus-secure-credential-endpoint";
  public static final String CALLER_TASK_QUEUE = "nexus-secure-credential-caller";
  public static final String HANDLER_TASK_QUEUE = "nexus-secure-credential-handler";

  /** Nexus routes, as {@code service/operation}, that require the end-user credential. */
  public static final Set<String> PROTECTED_ROUTES =
      Collections.singleton(TransferService.NAME + "/" + TransferService.TRANSFER);

  private SampleConfig() {}

  /**
   * Data converter that encrypts every payload, including failure messages and stack traces. The
   * context-aware codec must be the outermost (here, the only) codec.
   */
  public static DataConverter dataConverter(Aead payloadAead) {
    return new CodecDataConverter(
        DefaultDataConverter.newDefaultInstance(),
        Collections.singletonList(new ContextAwareTinkPayloadCodec(payloadAead)),
        true);
  }
}
