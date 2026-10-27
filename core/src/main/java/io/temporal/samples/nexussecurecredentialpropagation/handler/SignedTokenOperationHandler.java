package io.temporal.samples.nexussecurecredentialpropagation.handler;

import com.google.crypto.tink.Mac;
import io.nexusrpc.Header;
import io.nexusrpc.OperationException;
import io.nexusrpc.handler.HandlerException;
import io.nexusrpc.handler.OperationCancelDetails;
import io.nexusrpc.handler.OperationContext;
import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationStartDetails;
import io.nexusrpc.handler.OperationStartResult;
import io.temporal.nexus.Nexus;
import io.temporal.nexus.WorkflowHandle;
import io.temporal.nexus.WorkflowRunOperation;
import io.temporal.samples.nexussecurecredentialpropagation.common.EndUserPrincipal;
import io.temporal.samples.nexussecurecredentialpropagation.common.Frame;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * Runs an operation as a Workflow whose ID is derived from the verified principal and the input,
 * and returns an operation token signed with a handler-only key.
 *
 * <p>Every caller that presents the same principal and business input attaches to the same
 * Workflow, so retries and exact replays cause no second effect. The signed token stops a caller
 * from forging a token that names another user's Workflow in order to cancel it. Any holder of an
 * issued token can cancel, including an exact replayer; restricting holders is the job of the
 * endpoint's allowed callers.
 */
public final class SignedTokenOperationHandler<T, R> implements OperationHandler<T, R> {
  private static final String LABEL = "operation-token";
  private static final String VERSION = "v1";
  private static final int MAX_TOKEN_LENGTH = 1024;
  private static final int MAX_ID_LENGTH = 512;

  /** Derives the backing Workflow ID. Must not use the Nexus request ID. */
  public interface ResourceIdFunction<T> {
    String apply(EndUserPrincipal principal, T input);
  }

  /** Creates the handle that starts, or attaches to, the backing Workflow. */
  public interface HandleFactory<T, R> {
    WorkflowHandle<R> create(String workflowId, EndUserPrincipal principal, T input);
  }

  private final Mac mac;
  private final ResourceIdFunction<T> resourceIds;
  private final HandleFactory<T, R> handles;

  public SignedTokenOperationHandler(
      Mac mac, ResourceIdFunction<T> resourceIds, HandleFactory<T, R> handles) {
    this.mac = mac;
    this.resourceIds = resourceIds;
    this.handles = handles;
  }

  @Override
  public OperationStartResult<R> start(
      OperationContext context, OperationStartDetails details, T input) throws OperationException {
    EndUserPrincipal principal = RequestPrincipal.current();
    String workflowId = resourceIds.apply(principal, input);
    String token = sign(workflowId);
    // The SDK only adds its own (unsigned) token to the completion callback headers when they are
    // absent. The server uses these if the completion arrives before the start response, so they
    // must carry the signed token too.
    OperationStartDetails withToken =
        OperationStartDetails.newBuilder(details)
            .putCallbackHeader(Header.OPERATION_TOKEN, token)
            .putCallbackHeader("Nexus-Operation-Id", token)
            .build();
    OperationHandler<T, R> delegate =
        WorkflowRunOperation.fromWorkflowHandle(
            (ctx, startDetails, in) -> handles.create(workflowId, principal, in));
    delegate.start(context, withToken, input);
    return OperationStartResult.async(token);
  }

  @Override
  public void cancel(OperationContext context, OperationCancelDetails details) {
    String workflowId = verify(details.getOperationToken());
    Nexus.getOperationContext().getWorkflowClient().newUntypedWorkflowStub(workflowId).cancel();
  }

  String sign(String workflowId) {
    byte[] signed = Frame.of(LABEL).add(VERSION).add(workflowId).toByteArray();
    try {
      byte[] tag = mac.computeMac(signed);
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(Frame.of(VERSION).add(workflowId).add(tag).toByteArray());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  String verify(String token) {
    try {
      if (token == null || token.isEmpty() || token.length() > MAX_TOKEN_LENGTH) {
        throw new GeneralSecurityException("Invalid token length");
      }
      Frame.Reader reader = new Frame.Reader(Base64.getUrlDecoder().decode(token));
      reader.expect(VERSION);
      String workflowId = reader.nextString(MAX_ID_LENGTH);
      byte[] tag = reader.next(64);
      reader.expectEnd();
      mac.verifyMac(tag, Frame.of(LABEL).add(VERSION).add(workflowId).toByteArray());
      return workflowId;
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new HandlerException(
          HandlerException.ErrorType.BAD_REQUEST,
          "invalid operation token",
          (Throwable) null,
          HandlerException.RetryBehavior.NON_RETRYABLE);
    }
  }
}
